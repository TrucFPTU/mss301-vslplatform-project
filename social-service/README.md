# social-service (cổng 8084, DB `vsl_social`)

Blog & tương tác xã hội.

## Đã có sẵn (hạ tầng)
- Eureka, config, DB `vsl_social`, SecurityConfig.

## Bạn cần code (dưới `com.vsl.social`)
- `entity/`     → Blog, BlogComment, BlogLike, BlogShare, BlogReport, CommentReply, UserFollow, BlogNotification
  (tham chiếu người dùng bằng `Long userId`, KHÔNG `@ManyToOne User`)
- `repository/`, `dto/`, `service/`, `controller/`
- Nếu cần tên/avatar người dùng để hiển thị: gọi identity-service qua RestClient (`http://identity-service/...`).

## API Quản trị Báo cáo Vi phạm (Admin Blog Report Review)
Yêu cầu quyền `ADMIN` (qua API Gateway hoặc `X-User-Role: ROLE_ADMIN`).

- `GET /api/blogs/admin/reports` (hoặc `/api/admin/reports`): Danh sách báo cáo vi phạm phân trang, hỗ trợ query param `status=PENDING|RESOLVED|REJECTED`, `page`, `size`.
- `GET /api/blogs/admin/reports/{id}`: Chi tiết báo cáo vi phạm kèm thông tin toàn văn bài viết bị tố cáo.
- `PUT /api/blogs/admin/reports/{id}/resolve`: Admin duyệt xử lý vi phạm — tự động chuyển trạng thái bài viết sang `HIDDEN` và trạng thái báo cáo sang `RESOLVED`.
- `PUT /api/blogs/admin/reports/{id}/reject`: Admin bác bỏ báo cáo vi phạm — giữ nguyên bài viết và chuyển trạng thái báo cáo sang `REJECTED`.

## API Thống kê Mạng xã hội (Admin Social Analytics)
Yêu cầu quyền `ADMIN` (qua API Gateway hoặc `X-User-Role: ROLE_ADMIN`).

- `GET /api/blogs/admin/analytics/overview` (hoặc `/api/admin/social/analytics/overview`): Số liệu tổng quan bài viết (`total`, `published`, `hidden`, `draft`), tổng tương tác (`likes`, `comments`, `shares`), số báo cáo vi phạm (`pending`, `resolved`, `rejected`).
- `GET /api/blogs/admin/analytics/top-posts?limit=10`: Top bài viết có tổng lượt tương tác (`like + comment + share`) cao nhất.
- `GET /api/blogs/admin/analytics/top-creators?limit=5`: Top người dùng đóng góp nhiều bài viết và nhận được nhiều lượt tương tác nhất.


