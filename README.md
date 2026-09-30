# Document Management System - Backend API

Hệ thống Backend phục vụ quản lý dự án, lưu trữ tài liệu đa định dạng trên MinIO và tích hợp chức năng hỏi đáp thông minh dựa trên cơ chế AI RAG (Retrieval-Augmented Generation).


## Công nghệ sử dụng

- **Nền tảng:** Java 17+ / 23, Spring Boot 3.4.x
- **Bảo mật và xác thực:** Spring Security, JWT (JSON Web Token), phân quyền RBAC
- **Cơ sở dữ liệu:** PostgreSQL, PGVector, Spring Data JPA, Hibernate
- **Lưu trữ đối tượng:** MinIO Object Storage, hỗ trợ Presigned URL và Streaming
- **Tích hợp AI:** Spring RestClient kết nối dịch vụ Python FastAPI để trích xuất nội dung và tạo vector cho RAG
- **Tài liệu API:** OpenAPI 3, Swagger UI


## Tính năng chính

1. **Xác thực và quản lý người dùng**
   - Đăng ký, đăng nhập bằng JWT.
   - Quản lý thông tin tài khoản.
   - Tải lên và hiển thị ảnh đại diện.

2. **Quản lý dự án và thành viên**
   - Tạo và quản lý không gian dự án.
   - Phân quyền theo vai trò.
   - Mời thành viên tham gia dự án qua email.
   - Tải lên và quản lý logo dự án.

3. **Quản lý tài liệu**
   - Lưu trữ tài liệu đa định dạng trên MinIO.
   - Hỗ trợ PDF, Word, Excel, PowerPoint, hình ảnh, video, tệp nén và nhiều định dạng khác.
   - Hỗ trợ xem trước trực tuyến và tải tài liệu về.

4. **Tự động lập chỉ mục AI RAG**
   - Tự động nhận diện các định dạng tài liệu được hỗ trợ:
     - PDF
     - Word
     - Markdown
     - Text
   - Gửi tài liệu đến dịch vụ Python RAG để trích xuất nội dung và tạo vector.
   - Lưu trữ vector trong PGVector.
   - Các tệp đa phương tiện khác chỉ được lưu trữ trên MinIO mà không thực hiện trích xuất nội dung.

5. **Trợ lý AI hỏi đáp**
   - Tạo và quản lý phiên hội thoại.
   - Hỏi đáp dựa trên nội dung tài liệu được lựa chọn.
   - Giới hạn phạm vi truy xuất theo tài liệu và dự án.
   - Trả về câu trả lời kèm thông tin nguồn tham chiếu.


## Khởi chạy dự án

### 1. Yêu cầu môi trường

- Docker và Docker Compose
- JDK 17+ hoặc JDK 23
- Maven 3.9+

### 2. Khởi chạy PostgreSQL và MinIO

```bash
docker run -d --name postgres \
  -p 5432:5432 \
  -e POSTGRES_DB=document_management_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  pgvector/pgvector:pg16

docker run -d --name minio \
  -p 9000:9000 \
  -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  quay.io/minio/minio server /data \
  --console-address ":9001"
