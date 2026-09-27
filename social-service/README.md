# social-service (cổng 8084, DB `vsl_social`)

Blog & tương tác xã hội.

## Đã có sẵn (hạ tầng)
- Eureka, config, DB `vsl_social`, SecurityConfig.

## Bạn cần code (dưới `com.vsl.social`)
- `entity/`     → Blog, BlogComment, BlogLike, BlogShare, BlogReport, CommentReply, UserFollow, BlogNotification
  (tham chiếu người dùng bằng `Long userId`, KHÔNG `@ManyToOne User`)
- `repository/`, `dto/`, `service/`, `controller/`
- Nếu cần tên/avatar người dùng để hiển thị: gọi identity-service qua RestClient (`http://identity-service/...`).
