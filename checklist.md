# 📋 BACKEND API CHECKLIST - GAP ANALYSIS & IMPLEMENTATION PLAN
> **Căn cứ tài liệu:** [`docs_private/REQUIRE_ENDPOINT.MD`](file:///d:/Fsoft_OJT/Prepare_For_Training/document_management/docs_private/REQUIRE_ENDPOINT.MD)  
> **Dự án:** Quản lý Tài liệu & Tri thức AI (**KBase**)  
> **Thời gian phân tích:** 28/09/2026  
> **Trạng thái tổng thể:** Đã đối soát toàn bộ mã nguồn Backend hiện tại (`document_management`) với 28 RESTful endpoints theo yêu cầu các màn hình Frontend (`document_management_fe`).

---

## 📊 1. Tổng Quan Hiện Trạng Backend (Executive Summary)

| Chỉ số | Số lượng | Tỷ lệ (%) | Ghi chú |
| :--- | :---: | :---: | :--- |
| **Tổng số Endpoint FE yêu cầu** | **28** | **100%** | Theo 8 màn hình giao diện trong `REQUIRE_ENDPOINT.MD` |
| 🟢 **Đạt chuẩn (Sẵn sàng dùng)** | **0** | **0%** | Chưa có endpoint nào khớp 100% cả Path, DTO & Envelope |
| 🟡 **Cần chỉnh sửa (Refactor / Update)** | **8** | **28.6%** | Đã có controller/service nhưng lệch path, lệch DTO request/response hoặc chưa chuẩn Envelope |
| 🔴 **Cần viết mới hoàn toàn (New)** | **20** | **71.4%** | Chưa được triển khai (Dashboard, AI Chat, Member/Invite, Settings, Sessions) |

---

## 🛠️ 2. Checklist Các Hạng Mục Kỹ Thuật Nền Tảng (Core & Infrastructure)

Trước khi chỉnh sửa hoặc viết mới các endpoint nghiệp vụ, cần hoàn thiện các thành phần cốt lõi sau:

- [x] **[CORS-01] Bổ sung cấu hình CORS (Cross-Origin Resource Sharing):**
  - Đã cấu hình `CorsConfigurationSource` cho phép Origin Frontend (`http://localhost:5173`, `http://localhost:3000`), đầy đủ Headers và HTTP Methods.
- [ ] **[PORT-02] Thống nhất Server Port:**
  - *Hiện trạng:* `application.properties` đang để `server.port=10001`, trong khi tài liệu FE quy ước `http://localhost:8080/api/v1`.
  - *Cần làm:* Thống nhất giữ port `10001` (FE cấu hình env/Vite proxy) hoặc đổi lại `8080`.
- [ ] **[ENV-03] Chuẩn hóa Response Envelope (`ApiResponse<T>`):**
  - *Hiện trạng:* Đã bổ sung getter `getCode()` map trường `code`, cần bổ sung trường `meta` cho các API phân trang.
- [x] **[DB-04] Cập nhật và mở rộng Database Schema (Entities):**
  - Đã cập nhật 4 Entity hiện có và tạo mới 5 Entity kèm 5 Repository tương ứng. Xem chi tiết tại [Mục 4: Database Entities Checklist](#-4-checklist-mở-rộng-database--entities).

---

## 📑 3. Checklist Chi Tiết Endpoint Theo Từng Màn Hình Frontend

---

### 🔑 3.1. Màn Hình Xác Thực (Authentication - `Login.jsx`, `Register.jsx`)

| TT | Endpoint | Method | Trạng thái BE | Phân loại | Chi tiết cần chỉnh sửa / thêm mới |
| :---: | :--- | :---: | :---: | :---: | :--- |
| 1 | `/api/v1/auth/login` | `POST` | Đã xong | 🟢 **Đạt chuẩn** | • Đã có `accessToken`, `refreshToken`, `expiresIn` (tính bằng giây: 86400), `user` có `avatarUrl`. |
| 2 | `/api/v1/auth/register` | `POST` | Đã xong | 🟢 **Đạt chuẩn** | • Trả về HTTP `201 Created` kèm `code: 201` và DTO `RegisterResponse` có cả `id` và `userId` (dạng số). |
| 3 | `/api/v1/auth/me` | `GET` | Đã xong | 🟢 **Đạt chuẩn** | • Đã thêm endpoint bảo vệ với JWT, trả về đầy đủ `{ id, fullName, email, role, avatarUrl }`. |

---

### 🗂️ 3.2. Màn Hình Danh Sách Dự Án (Projects Hub - `ProjectsHub.jsx`)

| TT | Endpoint | Method | Trạng thái BE | Phân loại | Chi tiết cần chỉnh sửa / thêm mới |
| :---: | :--- | :---: | :---: | :---: | :--- |
| 4 | `/api/v1/projects` | `GET` | Đã xong | 🟢 **Đạt chuẩn** | • Đã bổ sung query params `search`, `role`, `status`.<br>• DTO `ProjectResponse` đầy đủ các trường `title`, `desc`, `role`, `status`, `storageUsed`, `storageLimit`, `activeMembers`, `updatedAt` (chuẩn ISO-8601: `2026-09-29T06:05:18.342Z`), `avatars`, `iconBg`, `iconColor`. |
| 5 | `/api/v1/projects` | `POST` | Đã xong | 🟢 **Đạt chuẩn** | • Nhận `name`/`title`, `description`, `maxFileSize`, `allowedFormats`, `inviteEmails`.<br>• Trả về HTTP `201 Created` kèm `code: 201`.<br>• Tự động tạo bản ghi lời mời thành viên ban đầu nếu có. |

---

### 📊 3.3. Màn Hình Tổng Quan Dự Án (Project Dashboard - `ProjectDashboard.jsx`)

| TT | Endpoint | Method | Trạng thái BE | Phân loại | Chi tiết cần chỉnh sửa / thêm mới |
| :---: | :--- | :---: | :---: | :---: | :--- |
| 6 | `/api/v1/projects/{projectId}/dashboard/stats` | `GET` | Chưa có | 🔴 **Thêm mới** | • Tạo endpoint thống kê chỉ số dự án: `total_files`, `storage_used`, `ai_queries`, `active_members`.<br>• Tính toán phân bố định dạng file (`formatDistribution`: PDF, Office, Markdown, Video, Ảnh...). |
| 7 | `/api/v1/projects/{projectId}/dashboard/recently-viewed` | `GET` | Chưa có | 🔴 **Thêm mới** | • Hỗ trợ query param `limit` (mặc định 5).<br>• Trả về danh sách file được truy cập/tải lên gần nhất trong dự án. |
| 8 | `/api/v1/projects/{projectId}/dashboard/activities` | `GET` | Chưa có | 🔴 **Thêm mới** | • Hỗ trợ query param `limit` (mặc định 10).<br>• Trả về timeline dòng hoạt động gần đây của dự án (user upload, vector index, AI query). |

---

### 📁 3.4. Màn Hình Quản Lý Tài Liệu (Project Documents - `ProjectDocuments.jsx`)

| TT | Endpoint | Method | Trạng thái BE | Phân loại | Chi tiết cần chỉnh sửa / thêm mới |
| :---: | :--- | :---: | :---: | :---: | :--- |
| 9 | `/api/v1/projects/{projectId}/documents` | `GET` | Đã có | 🟡 **Chỉnh sửa** | • Bổ sung query param `category` (`all`, `docs`, `sheets`, `media`, `images`, `code`), `sortBy` (`name`, `size`, `updatedAt`), `sortOrder` (`asc`, `desc`).<br>• Cấu trúc lại response: bọc ngoài `{ summary: { totalFiles, totalSize, counts: {...} }, files: [...] }` kèm phân trang `meta`.<br>• File item bổ sung: `format`, `sizeBytes`, `category`, `author`, `authorInitials`, `authorColor`, `minioKey`. |
| 10 | `/api/v1/projects/{projectId}/documents/upload` | `POST` | Đã có | 🟡 **Chỉnh sửa** | • Bổ sung param `category` (optional) vào request form.<br>• Validate dung lượng file với `maxFileSize` của dự án và định dạng với `allowedFormats`.<br>• Trả về status `201 Created` và cấu trúc response: `{ id, name, size, format, minioUrl }`. |
| 11 | `/api/v1/projects/{projectId}/documents/{documentId}/preview-url` | `GET` | Chưa có | 🔴 **Thêm mới** | • Tạo endpoint cấp link preview trực tiếp từ MinIO kèm chữ ký (Presigned URL) dạng JSON response: `{ documentId, previewUrl, mimeType, expiresIn }`.<br>*(Hiện tại BE chỉ có `/api/v1/documents/{documentId}/preview` stream binary trực tiếp qua Spring Boot)*. |
| 12 | `/api/v1/projects/{projectId}/documents/{documentId}/download-url` | `GET` | Chưa có | 🔴 **Thêm mới** | • Tạo endpoint sinh Presigned URL tải file: `{ downloadUrl, fileName, expiresIn }`.<br>*(Khắc phục việc hiện tại BE dùng `/documents/{id}/download?presigned=true` không đúng path REST của FE)*. |
| 13 | `/api/v1/projects/{projectId}/documents/{documentId}` | `DELETE` | Một phần | 🟡 **Chỉnh sửa** | • Thêm ánh xạ đường dẫn có `{projectId}`: `/api/v1/projects/{projectId}/documents/{documentId}` (hiện tại BE chỉ có `/api/v1/documents/{documentId}`).<br>• Kiểm tra quyền thành viên trong dự án trước khi xóa. |
| 14 | `/api/v1/projects/{projectId}/documents/bulk-delete` | `POST` | Chưa có | 🔴 **Thêm mới** | • Endpoint xóa hàng loạt tệp: nhận body `{ "ids": [1, 2, 3] }`.<br>• Xóa đồng loạt file trên MinIO và metadata trong DB, trả về `{ deletedCount: N }`. |

---

### 🤖 3.5. Màn Hình Trợ Lý Hỏi Đáp AI / RAG (Project Chat - `ProjectChat.jsx`)

| TT | Endpoint | Method | Trạng thái BE | Phân loại | Chi tiết cần chỉnh sửa / thêm mới |
| :---: | :--- | :---: | :---: | :---: | :--- |
| 15 | `/api/v1/projects/{projectId}/chat/sessions` | `GET` | Chưa có | 🔴 **Thêm mới** | • Lấy danh sách phiên chat trong dự án theo người dùng: `[{ id, title, updatedAt, messageCount }]`. |
| 16 | `/api/v1/projects/{projectId}/chat/sessions` | `POST` | Chưa có | 🔴 **Thêm mới** | • Tạo phiên chat mới với body `{ "initialQuery": "..." }`.<br>• Tự sinh tiêu đề phiên chat từ câu hỏi ban đầu, trả về status `201 Created`. |
| 17 | `/api/v1/projects/{projectId}/chat/sessions/{sessionId}/messages` | `GET` | Chưa có | 🔴 **Thêm mới** | • Lấy toàn bộ lịch sử tin nhắn trong phiên chat (bao gồm tin user và câu trả lời AI kèm citations). |
| 18 | `/api/v1/projects/{projectId}/chat/sessions/{sessionId}/messages` | `POST` | Chưa có | 🔴 **Thêm mới** | • Gửi câu hỏi vào phiên chat: body `{ "message": "...", "selectedDocumentIds": [1, 2] }`.<br>• Tích hợp `AiIntegrationService` / RAG Service để phản hồi kèm nguồn trích dẫn (`citation`: fileName, documentId, page, confidence, snippet).<br>• Hỗ trợ Response JSON hoặc SSE Stream (`text/event-stream`). |

---

### 👥 3.6. Màn Hình Quản Lý Thành Viên & Phân Quyền (Project Members - `ProjectMembers.jsx`)

| TT | Endpoint | Method | Trạng thái BE | Phân loại | Chi tiết cần chỉnh sửa / thêm mới |
| :---: | :--- | :---: | :---: | :---: | :--- |
| 19 | `/api/v1/projects/{projectId}/members` | `GET` | Chưa có | 🔴 **Thêm mới** | • Lấy danh sách thành viên dự án, hỗ trợ param `search` (tên/email).<br>• Trả về: `id`, `name`, `email`, `role`, `avatarBg`, `initial`, `joinedDate`, `contributions` (số tệp upload). |
| 20 | `/api/v1/projects/{projectId}/invites` | `GET` | Chưa có | 🔴 **Thêm mới** | • Lấy danh sách lời mời đang chờ (Pending Invites): `[{ id, email, role, sentDate, expiresIn }]`. |
| 21 | `/api/v1/projects/{projectId}/invites` | `POST` | Chưa có | 🔴 **Thêm mới** | • Gửi lời mời thành viên qua email: body `{ "email": "...", "role": "Member" }`.<br>• Sinh invite token, hạn sử dụng, trả về status `201 Created`. |
| 22 | `/api/v1/projects/{projectId}/members/{memberId}/role` | `PATCH` | Chưa có | 🔴 **Thêm mới** | • Cập nhật vai trò thành viên trong dự án: body `{ "role": "Admin" / "Member" / "Viewer" }`.<br>• Kiểm tra quyền Project Owner/Admin. |
| 23 | `/api/v1/projects/{projectId}/invites/{inviteId}/resend` | `POST` | Chưa có | 🔴 **Thêm mới** | • Gửi lại email lời mời tham gia dự án. |
| 24 | `/api/v1/projects/{projectId}/invites/{inviteId}` | `DELETE` | Chưa có | 🔴 **Thêm mới** | • Hủy lời mời đang chờ xử lý. |
| 25 | `/api/v1/projects/{projectId}/members/{memberId}` | `DELETE` | Chưa có | 🔴 **Thêm mới** | • Xóa thành viên khỏi dự án. Không cho phép xóa Project Owner. |

---

### ⚙️ 3.7. Màn Hình Cài Đặt Dự Án (Project Settings - `ProjectSettings.jsx`)

| TT | Endpoint | Method | Trạng thái BE | Phân loại | Chi tiết cần chỉnh sửa / thêm mới |
| :---: | :--- | :---: | :---: | :---: | :--- |
| 26 | `/api/v1/projects/{projectId}/settings` | `GET` | Chưa có | 🔴 **Thêm mới** | • Lấy cấu hình chi tiết dự án: `projectName`, `projectDesc`, `logoUrl`, `minioBucket`, `storageUsedBytes`, `storageLimitBytes`, `maxFileSize`, `allowedFormats`, `aiPersona: { temperature, systemPrompt }`. |
| 27 | `/api/v1/projects/{projectId}/settings` | `PUT` | Chưa có | 🔴 **Thêm mới** | • Cập nhật cấu hình dự án, bao gồm giới hạn tệp, định dạng cho phép và cấu hình AI Persona (`temperature`, `systemPrompt`). |
| 28 | `/api/v1/projects/{projectId}/transfer-ownership` | `POST` | Chưa có | 🔴 **Thêm mới** | • Chuyển quyền Project Owner sang cho email khác: body `{ "newOwnerEmail": "..." }`.<br>• Chỉ Owner hiện tại mới có quyền gọi. |
| 29 | `/api/v1/projects/{projectId}/archive` | `POST` | Chưa có | 🔴 **Thêm mới** | • Chuyển trạng thái dự án sang lưu trữ (`status = archived` - Read only). |
| 30 | `/api/v1/projects/{projectId}` | `DELETE` | Đã có | 🟡 **Chỉnh sửa** | • Bổ sung kiểm tra xác nhận an toàn: Body `{ "confirmationProjectName": "..." }` phải trùng khớp với tên dự án trước khi thực hiện xóa vĩnh viễn dữ liệu MinIO & DB. |

---

### 👤 3.8. Màn Hình Hồ Sơ Cá Nhân (User Profile - `UserProfile.jsx`)

| TT | Endpoint | Method | Trạng thái BE | Phân loại | Chi tiết cần chỉnh sửa / thêm mới |
| :---: | :--- | :---: | :---: | :---: | :--- |
| 31 | `/api/v1/users/me/profile` | `GET` | Chưa có | 🔴 **Thêm mới** | • Lấy thông tin hồ sơ mở rộng của người dùng hiện tại: `fullName`, `title`, `phone`, `role`, `initials`, `avatarUrl`. |
| 32 | `/api/v1/users/me/profile` | `PUT` | Chưa có | 🔴 **Thêm mới** | • Cập nhật hồ sơ cá nhân: `{ fullName`, `title`, `phone }`. |
| 33 | ~~/api/v1/users/me/preferences~~ | ~~PUT~~ | Đã hủy | ⚪ **Đã loại bỏ** | *(Đã hủy theo yêu cầu - không lưu theme/language/timezone ở BE)* |
| 34 | `/api/v1/users/me/change-password` | `POST` | Chưa có | 🔴 **Thêm mới** | • Tách riêng API đổi mật khẩu: `{ currentPassword, newPassword }` (thay vì gộp chung trong `PUT /users/me`). |
| 35 | `/api/v1/users/me/sessions` | `GET` | Chưa có | 🔴 **Thêm mới** | • Lấy danh sách thiết bị/phiên đăng nhập hiện tại: `[{ id, deviceName, location, ip, isCurrent, deviceType, lastActive }]`. |
| 36 | `/api/v1/users/me/sessions/{sessionId}` | `DELETE` | Chưa có | 🔴 **Thêm mới** | • Thu hồi một phiên đăng nhập thiết bị cụ thể. |
| 37 | `/api/v1/users/me/sessions` | `DELETE` | Chưa có | 🔴 **Thêm mới** | • Đăng xuất khỏi tất cả các thiết bị khác (ngoại trừ phiên hiện tại). |

---

## 🗄️ 4. Checklist Mở Rộng Database & Entities

Để các endpoint trên hoạt động đầy đủ, hệ thống cần bổ sung và cập nhật các Entity sau:

### 4.1. Cập nhật các Entity hiện có:
- [x] **`User.java`**:
  - `title` *(String)*: Chức danh chuyên môn (VD: "Lead Solution Architect").
  - `phone` *(String)*: Số điện thoại.
  - `avatarUrl` *(String)*: Đường dẫn ảnh đại diện.
  - *(Đã loại bỏ các trường theme, language, timezone theo yêu cầu)*.
- [x] **`Project.java`**:
  - `status` *(String, default: "active")*: `active` hoặc `archived`.
  - `maxFileSize` *(String, default: "50 MB")*: Giới hạn kích thước file.
  - `allowedFormats` *(String, default: "pdf,docx,xlsx,pptx,md,txt,images")*: Danh sách định dạng cho phép.
  - `aiTemperature` *(Double, default: 0.2)*: Nhiệt độ sáng tạo của AI.
  - `aiSystemPrompt` *(String, columnDefinition="TEXT")*: Prompt chỉ thị hệ thống cho AI Assistant.
  - `logoUrl` *(String)*: Logo dự án.
  - `storageLimitBytes` *(Long, default: 10737418240L)*: Hạn mức lưu trữ (10 GB).
  - `createdAt` *(Instant)* & `updatedAt` *(Instant)*.
- [x] **`ProjectMember.java`**:
  - `joinedAt` *(Instant, default: now)*: Thời điểm tham gia dự án.
- [x] **`DocumentMetadata.java`**:
  - `category` *(String)*: Nhóm tệp (`docs`, `sheets`, `media`, `images`, `code`).
  - `updatedAt` *(Instant)*.

### 4.2. Tạo mới các Entity:
- [x] **`ProjectInvite.java`**: Quản lý lời mời tham gia dự án (`id`, `project`, `email`, `role`, `token`, `status`, `sentDate`, `expiresAt`).
- [x] **`ChatSession.java`**: Phiên trò chuyện AI trong dự án (`id`, `project`, `user`, `title`, `createdAt`, `updatedAt`).
- [x] **`ChatMessage.java`**: Tin nhắn trong phiên chat (`id`, `chatSession`, `sender` ("user"|"ai"), `content`, `intro`, `stepsJson`, `citationJson`, `createdAt`).
- [x] **`UserSession.java`**: Quản lý phiên đăng nhập theo thiết bị (`id`, `user`, `refreshTokenHash`, `deviceName`, `ipAddress`, `location`, `deviceType`, `isCurrent`, `lastActive`, `createdAt`).
- [x] **`ProjectActivity.java`**: Dòng hoạt động dự án (`id`, `project`, `user`, `userAction`, `target`, `createdAt`).

---

## 🚀 5. Lộ Trình Triển Khai Đề Xuất (Implementation Phases)

### 📌 Giai đoạn 1: Chuẩn hóa Hạ tầng, Xác thực & Hồ sơ cá nhân (Ưu tiên số 1)
> *Mục tiêu: Đảm bảo Frontend đăng nhập, lấy được phiên làm việc và gọi API mà không bị chặn CORS.*
1. Cấu hình CORS (`CorsConfigurationSource`) và chuẩn hóa format `ApiResponse`.
2. Chỉnh sửa `POST /auth/login`, `POST /auth/register`.
3. Thêm mới `GET /auth/me`.
4. Mở rộng `User` entity và triển khai nhóm API `User Profile` (`/users/me/profile`, `/preferences`, `/change-password`).

### 📌 Giai đoạn 2: Quản lý Dự án & Thành viên (Projects Hub & Members)
> *Mục tiêu: Cho phép người dùng tạo, xem danh sách dự án, cấu hình dự án và mời cộng tác viên.*
1. Mở rộng `Project` entity (`status`, `allowedFormats`, `maxFileSize`, `aiPersona`).
2. Nâng cấp `GET /projects` (bộ lọc search, role, status) và `POST /projects`.
3. Triển khai nhóm API cài đặt dự án `Project Settings` (`GET/PUT /settings`, `archive`, `transfer-ownership`).
4. Triển khai nhóm API thành viên `Project Members` & `Project Invites` (mời thành viên, duyệt lời mời, đổi vai trò, xóa thành viên).

### 📌 Giai đoạn 3: Quản lý Tài liệu & MinIO Presigned URLs (Project Documents)
> *Mục tiêu: Đáp ứng trọn vẹn màn hình Explorer, phân loại tệp, preview và download qua Presigned URL.*
1. Thêm `category` vào `DocumentMetadata` và logic tự phân loại định dạng tệp.
2. Nâng cấp `GET /projects/{projectId}/documents` trả về `summary` counts + format chips + files.
3. Viết mới API Presigned URL: `preview-url` và `download-url`.
4. Viết mới API xóa hàng loạt `POST /projects/{projectId}/documents/bulk-delete`.

### 📌 Giai đoạn 4: Bảng Thống kê & Dòng Hoạt động (Project Dashboard)
> *Mục tiêu: Cung cấp đầy đủ dữ liệu cho trang tổng quan Dashboard.*
1. Tạo API `GET /dashboard/stats` (tính toán dung lượng, số tệp, % định dạng tài liệu).
2. Tạo API `GET /dashboard/recently-viewed`.
3. Tạo API `GET /dashboard/activities`.

### 📌 Giai đoạn 5: Trợ Lý Tri Thức AI RAG (Project Chatbot)
> *Mục tiêu: Hoàn thiện tính năng hỏi đáp thông minh dựa trên ngữ cảnh tài liệu dự án.*
1. Tạo bảng và Repository cho `ChatSession` & `ChatMessage`.
2. Triển khai API quản lý phiên chat (`GET/POST /chat/sessions`).
3. Triển khai API nhắn tin và tích hợp AI RAG kèm trích dẫn nguồn (`POST /chat/sessions/{sessionId}/messages`).
