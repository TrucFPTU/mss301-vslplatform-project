-- Dọn tên từ vựng còn ký tự thừa lấy từ nhãn bộ dữ liệu gốc (" !", "= 24", khoảng trắng trong ngoặc, "a/ b").
-- Chỉ đổi phần HIỂN THỊ; chấm điểm dùng expected_id / vocabulary_id nên không ảnh hưởng.
-- An toàn khi chạy lại: mỗi dòng chỉ sửa khi tên vẫn còn đúng giá trị cũ.
--   Dev:  docker exec -i vsl-postgres psql -U vsl -d vsl_learning < scripts/clean_vocabulary_words.sql
--   Máy ảo: ssh vsl-vm "docker exec -i vsl-postgres psql -U vsl -d vsl_learning" < scripts/clean_vocabulary_words.sql
BEGIN;

UPDATE vocabularies v SET word = m.new_word
FROM (VALUES
    (7,  'nghé ( trâu con )',            'nghé (trâu con)'),
    (10, 'nắm bắt !',                    'nắm bắt'),
    (13, 'chia sẻ !',                    'chia sẻ'),
    (15, 'ru em bé ngủ !',               'ru em bé ngủ'),
    (17, 'dễ thương !',                  'dễ thương'),
    (25, 'buffe / gắp thức ăn',          'buffet / gắp thức ăn'),
    (27, 'quầy hoa quả !',               'quầy hoa quả'),
    (28, 'miếng/ bún',                   'miếng / bún'),
    -- id 33 đã là "bao nhiêu" (ký hiệu khác, expected_id 66) -> đặt tên phân biệt thay vì trùng
    (30, 'bao nhiêu !',                  'bao nhiêu (cách 2)'),
    (31, 'thứ 3 là ( miêu tả thứ tự )',  'thứ 3 là (miêu tả thứ tự)'),
    (32, '1 ít !',                       'một ít'),
    (36, 'em trai ! = 24',               'em trai'),
    (37, 'thân thiết/ bạn thân',         'thân thiết / bạn thân'),
    (38, 'địa cầu !',                    'địa cầu'),
    (41, 'cái cây/ phát biểu',           'cái cây / phát biểu'),
    (43, 'lịch sử !',                    'lịch sử')
) AS m(id, old_word, new_word)
WHERE v.id = m.id AND v.word = m.old_word;

-- Kiểm tra: không còn tên nào có " !" / "= " / khoảng trắng trong ngoặc / "a/ b".
SELECT id, word FROM vocabularies
WHERE word ~ '( !|= |\( | \)|\S/|/\S)' ORDER BY id;

COMMIT;
