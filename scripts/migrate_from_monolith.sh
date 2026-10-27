#!/usr/bin/env bash
# Chuyển dữ liệu từ DB monolith cũ (1 DB) sang 3 DB của microservice, GIỮ NGUYÊN ID
# (userId / vocabularyId được các service tham chiếu chéo nên phải khớp).
#
# Yêu cầu:
#   - Container Postgres cũ đang chạy:  OLD_PG (mặc định mig-oldpg), DB vsl_db, user vsl_user
#   - Stack mới đang chạy (vsl-postgres), các service identity/learning/social ĐÃ DỪNG
# Script XÓA SẠCH dữ liệu hiện có trong 3 DB mới trước khi nhập (chỉ dùng cho lần chuyển ban đầu).
#
# Thời gian: monolith ghi theo giờ Việt Nam (JVM Asia/Ho_Chi_Minh); microservice cũng chạy giờ VN,
# nên cột timestamp giữ nguyên, cột timestamptz (social) được gắn múi giờ VN.
set -euo pipefail

OLD_PG=${OLD_PG:-mig-oldpg}
OLD="docker exec -i $OLD_PG psql -v ON_ERROR_STOP=1 -U vsl_user -d vsl_db -Atq"
new() { docker exec -i vsl-postgres psql -v ON_ERROR_STOP=1 -U vsl -d "$1" -Atq "${@:2}"; }

# copy <db mới> <bảng(cột,...)> <câu SELECT trên DB cũ>
copy() {
  local db=$1 target=$2 select=$3
  $OLD -c "\copy ($select) to stdout with (format csv)" | new "$db" -c "\copy $target from stdin with (format csv)"
  echo "  $db.$target <- $(new "$db" -c "select count(*) from ${target%%(*}") dòng"
}

reset_seq() {  # đặt sequence id = max(id) để bản ghi tạo mới không trùng id đã nhập
  local db=$1 table=$2 col=${3:-id}
  new "$db" -c "select setval(pg_get_serial_sequence('$table','$col'), coalesce((select max($col) from $table), 0) + 1, false)" >/dev/null
}

VN="'Asia/Ho_Chi_Minh'"
# Từ rác do Postman smoke test tạo ở monolith (không có lịch sử luyện tập, trùng expectedId với từ thật).
JUNK_VOCAB="word like '\_postman\_smoke\_test%'"

echo "== identity"
new vsl_identity -c "TRUNCATE refresh_tokens, users RESTART IDENTITY CASCADE"
# refresh token KHÔNG chuyển: bản mới lưu dạng hash -> người dùng đăng nhập lại.
copy vsl_identity "users(user_id,username,email,password_hash,full_name,avatar_url,role,status,last_login,email_notifications_enabled,created_at,updated_at)" \
  "select user_id, username, lower(email), password_hash, coalesce(full_name, username), avatar_url, role,
          coalesce(status,'ACTIVE'), last_login, false, coalesce(created_at, now()), coalesce(updated_at, created_at, now())
   from users order by user_id"
reset_seq vsl_identity users user_id

echo "== learning"
new vsl_learning -c "TRUNCATE vocabulary_suggestions, user_achievements, user_progress, attempt_history, visit_logs,
                     vocabularies, categories, achievements RESTART IDENTITY CASCADE"
copy vsl_learning "categories(id,name,description,image_url)" "select id, name, description, image_url from categories order by id"
copy vsl_learning "vocabularies(id,category_id,word,description,video_tutorial_url,image_url,expected_id)" \
  "select id, category_id, word, description, video_tutorial_url, image_url, expected_id from vocabularies where not ($JUNK_VOCAB) order by id"
copy vsl_learning "achievements(id,achievement_key,name,description,icon_key)" \
  "select id, achievement_key, name, description, icon_key from achievements order by id"
copy vsl_learning "user_achievements(id,user_id,achievement_id,unlocked_at)" \
  "select id, user_id, achievement_id, unlocked_at from user_achievements order by id"
copy vsl_learning "user_progress(id,user_id,vocabulary_id,learning_status,last_attempted_at)" \
  "select id, user_id, vocabulary_id, learning_status, last_attempted_at from user_progress order by id"
copy vsl_learning "attempt_history(id,user_id,vocabulary_id,is_correct,ai_predicted_code,confidence,attempted_at)" \
  "select id, user_id, vocabulary_id, is_correct, ai_predicted_code, confidence, attempted_at from attempt_history order by id"
copy vsl_learning "visit_logs(id,user_id,session_id,device_info,ip_address,location,visited_at)" \
  "select id, user_id, session_id, device_info, ip_address, location, visited_at from visit_logs order by id"
for t in categories vocabularies achievements user_achievements user_progress attempt_history visit_logs; do reset_seq vsl_learning $t; done

echo "== social"
new vsl_social -c "TRUNCATE blog_notifications, blog_shares, reply_likes, comment_likes, comment_replies, blog_reports,
                   blog_likes, blog_comments, user_follows, blogs RESTART IDENTITY CASCADE"
# REMOVED (monolith) = HIDDEN (bản mới). Bộ đếm like/comment tính lại từ dữ liệu thật.
# normalized_title / content_fingerprint để NULL: social tự tính khi khởi động (BlogFingerprintBackfill).
copy vsl_social "blogs(id,author_id,title,content,thumbnail_url,status,deletion_reason,like_count,comment_count,share_count,created_at,updated_at)" \
  "select b.id, b.author_id, b.title, b.content, b.thumbnail_url,
          case b.status when 'REMOVED' then 'HIDDEN' else b.status end, b.deletion_reason,
          (select count(*) from blog_likes l where l.blog_id = b.id),
          (select count(*) from blog_comments c where c.blog_id = b.id), 0,
          b.created_at at time zone $VN, coalesce(b.updated_at, b.created_at) at time zone $VN
   from blogs b order by b.id"
copy vsl_social "blog_comments(id,blog_id,user_id,content,created_at,updated_at)" \
  "select id, blog_id, user_id, content, created_at at time zone $VN, created_at at time zone $VN from blog_comments order by id"
copy vsl_social "blog_likes(id,blog_id,user_id,created_at,updated_at)" \
  "select id, blog_id, user_id, created_at at time zone $VN, created_at at time zone $VN from blog_likes order by id"
copy vsl_social "blog_reports(id,blog_id,reporter_id,reason,status,created_at,updated_at)" \
  "select id, blog_id, reporter_id, coalesce(reason, ''), status, created_at at time zone $VN,
          coalesce(resolved_at, created_at) at time zone $VN from blog_reports order by id"
for t in blogs blog_comments blog_likes blog_reports; do reset_seq vsl_social $t; done

echo "== xong"
