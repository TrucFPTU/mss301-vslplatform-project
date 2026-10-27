# VSL Platform - Tài Liệu API Quản Trị Admin & Thống Kê Analytics
> **Dành cho:** Thành viên thứ 5 (Quản trị Admin & Thống kê Analytics / Hỗ trợ Learning & Social)  
> **Cổng truy cập tập trung:** `http://localhost:8080` (API Gateway)  
> **Yêu cầu phân quyền:** Header `Authorization: Bearer <adminToken>` hoặc role `ROLE_ADMIN`.

---

## 1. Hướng Dẫn Sử Dụng Collection & Swagger

Dự án đã đóng gói sẵn 2 file tài liệu để nhóm có thể import và chạy ngay:

1. **Postman Collection v2.1.0:** [vsl-admin-api-collection.json](file:///d:/3.FPT/MSS/mss301-vslplatform-project/vsl-admin-api-collection.json)  
   - Mở Postman $\rightarrow$ Chọn **Import** $\rightarrow$ Kéo thả file `vsl-admin-api-collection.json`.  
   - Tất cả 17 endpoints đã được gom nhóm vào 4 thư mục rõ ràng.  
   - **Biến môi trường tích hợp sẵn:**  
     - `baseUrl`: `http://localhost:8080`  
     - `adminToken`: Token JWT mẫu với quyền `ROLE_ADMIN`.  
   - Cơ chế Auth: Tự động kế thừa `Bearer Token` từ collection cho mọi request con.

2. **OpenAPI / Swagger Spec:** [vsl-admin-openapi.json](file:///d:/3.FPT/MSS/mss301-vslplatform-project/vsl-admin-openapi.json)  
   - Dùng để import trực tiếp vào [editor.swagger.io](https://editor.swagger.io/) hoặc tích hợp vào Swagger UI.

---

## 2. Danh Mục Các API Admin Đã Triển Khai

### Nhóm 1: Thống Kê Học Tập (Learning Analytics - `learning-service`)

| Method | Endpoint | Mô tả | Query Params |
|---|---|---|---|
| `GET` | `/api/practice/analytics/overview` | Số liệu tổng quan: tổng từ vựng, danh mục, lượt nộp bài, số học viên, tỉ lệ chính xác chung | Không |
| `GET` | `/api/practice/analytics/trends` | Biểu đồ xu hướng luyện tập theo ngày (số lượt nộp, đúng/sai, số học viên active) | `days` (mặc định 7) |
| `GET` | `/api/practice/analytics/categories` | Thống kê tiến độ theo danh mục: số từ vựng, số lượt làm bài, số từ đã thuộc | Không |
| `GET` | `/api/practice/analytics/vocabulary-insights` | Danh sách Top các từ hay làm sai nhất và Top các từ thành thạo nhất | `limit` (mặc định 5) |

---

### Nhóm 2: Quản Trị Danh Mục Thành Tựu (Admin Achievements - `learning-service`)

| Method | Endpoint | Mô tả | Request Body / Query |
|---|---|---|---|
| `GET` | `/api/practice/admin/achievements` | Danh sách toàn bộ thành tựu trong hệ thống kèm số người đã mở khoá | Không |
| `GET` | `/api/practice/admin/achievements/{id}` | Xem chi tiết một thành tựu theo ID | Path `id` |
| `POST` | `/api/practice/admin/achievements` | Tạo mới thành tựu (tự sinh ID an toàn, chống trùng key) | JSON: `key`, `name`, `description`, `iconKey` |
| `PUT` | `/api/practice/admin/achievements/{id}` | Cập nhật thông tin thành tựu (tên, mô tả, icon) | JSON: `key`, `name`, `description`, `iconKey` |
| `DELETE` | `/api/practice/admin/achievements/{id}` | Xoá thành tựu và dọn dẹp các bản ghi liên quan | Path `id` |
| `POST` | `/api/practice/admin/achievements/{id}/grant` | Mở khoá / Trao tặng thủ công thành tựu cho một học viên | Query `userId` |

---

### Nhóm 3: Duyệt Báo Cáo Vi Phạm Bài Viết (Admin Blog Reports - `social-service`)

| Method | Endpoint | Mô tả | Request Body / Query |
|---|---|---|---|
| `GET` | `/api/blogs/admin/reports` | Danh sách báo cáo vi phạm phân trang, lọc theo trạng thái | `status` (PENDING/RESOLVED/REJECTED), `page`, `size` |
| `GET` | `/api/blogs/admin/reports/{id}` | Xem chi tiết báo cáo kèm toàn văn tiêu đề, nội dung bài viết bị tố cáo | Path `id` |
| `PUT` | `/api/blogs/admin/reports/{id}/resolve` | Phê duyệt vi phạm: Tự động chuyển bài viết sang `HIDDEN` và báo cáo sang `RESOLVED` | Path `id` |
| `PUT` | `/api/blogs/admin/reports/{id}/reject` | Bác bỏ báo cáo sai: Giữ nguyên bài viết công khai và chuyển báo cáo sang `REJECTED` | Path `id` |

---

### Nhóm 4: Thống Kê Mạng Xã Hội (Admin Social Analytics - `social-service`)

| Method | Endpoint | Mô tả | Query Params |
|---|---|---|---|
| `GET` | `/api/blogs/admin/analytics/overview` | Tổng quan: tổng số bài (`published`, `hidden`, `draft`), tổng tương tác (`likes`, `comments`, `shares`), số báo cáo vi phạm (`pending`, `resolved`, `rejected`) | Không |
| `GET` | `/api/blogs/admin/analytics/top-posts` | Top bài viết có tổng lượt tương tác (`like + comment + share`) cao nhất | `limit` (mặc định 10) |
| `GET` | `/api/blogs/admin/analytics/top-creators` | Top người dùng đóng góp nhiều bài viết hữu ích và nhận nhiều tương tác nhất | `limit` (mặc định 5) |

---

### Nhóm 5: Quản Trị Người Dùng (Admin User Management - `identity-service`)

| Method | Endpoint | Mô tả | Request Body / Query |
|---|---|---|---|
| `GET` | `/api/users/admin` | Danh sách người dùng hệ thống phân trang, tìm kiếm từ khoá, lọc theo role, status | `keyword`, `role`, `status`, `page`, `size` |
| `GET` | `/api/users/admin/statistics` | Thống kê số lượng tài khoản (tổng số, hoạt động, bị khoá, admin, user) | Không |
| `GET` | `/api/users/admin/{id}` | Xem thông tin chi tiết một tài khoản theo ID | Path `id` |
| `POST` | `/api/users/admin` | Admin tạo tài khoản người dùng / quản trị viên mới | JSON: `username`, `email`, `password`, `fullName`, `role` |
| `PUT` | `/api/users/admin/{id}/status` | Khoá (`BANNED`) hoặc mở khoá (`ACTIVE`) tài khoản người dùng | JSON: `status`, `reason` |
| `PUT` | `/api/users/admin/{id}/role` | Phân quyền vai trò tài khoản (`ROLE_USER` $\leftrightarrow$ `ROLE_ADMIN`) | JSON: `role` |

---

## 3. Cấu Trúc Envelope Chuẩn (`ApiResponse`)

Tất cả các API trên đều tuân thủ cấu trúc envelope thống nhất toàn hệ thống:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Thành công",
  "data": { ... },
  "timestamp": "2026-10-15T19:30:00Z"
}
```
