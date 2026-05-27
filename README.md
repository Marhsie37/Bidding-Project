# Hệ thống Đấu giá Trực tuyến (Auction System)

## 1. Mô tả bài toán và phạm vi hệ thống

Hệ thống bidding (đấu giá trực tuyến) là một nền tảng phần mềm theo mô hình **Client - Server**, cho phép nhiều người dùng cùng tham gia cạnh tranh giá để mua một sản phẩm hoặc dịch vụ trong một khoảng thời gian xác định. Thay vì bán với giá cố định, người bán (Seller) đưa sản phẩm lên hệ thống và giá bán cuối cùng được xác định thông qua quá trình đấu giá công khai giữa các người mua (Bidder). 

**Phạm vi hệ thống:**
- Hỗ trợ đa người dùng kết nối đồng thời qua mạng (Socket/TCP).
- Đảm bảo tính nhất quán của dữ liệu và xử lý tranh chấp (Thread Race Condition) khi nhiều người dùng cùng đặt giá.
- Hệ thống thông báo thời gian thực (Real-time Notification) tới các thiết bị khi có lượt đặt giá mới hoặc khi phiên đấu giá kết thúc.

## 2. Công nghệ sử dụng và Yêu cầu cài đặt

**Công nghệ sử dụng:**
- **Ngôn ngữ:** Java (JDK 17 trở lên)
- **Giao diện người dùng (GUI):** JavaFX 21
- **Quản lý dự án & Build tool:** Maven
- **Cơ sở dữ liệu (Production):** MySQL 8+
- **Cơ sở dữ liệu (Testing):** H2 In-memory Database
- **Connection Pool:** HikariCP
- **Xử lý JSON:** Gson

**Yêu cầu cài đặt & Môi trường chạy:**
- Máy tính cần cài đặt **Java Development Kit (JDK) 17+**.
- Cài đặt **Maven** (có thể sử dụng Maven Wrapper hoặc Maven tích hợp sẵn trong IDE).
- Cài đặt **MySQL Server** đang chạy trên port 3306.
  - Tạo một schema (database) tên là `auction_db` (hoặc cấu hình lại URL trong `DatabaseConnection.java`).
  - Username mặc định: `root`
  - Password mặc định: `admin`

## 3. Cấu trúc thư mục (Modules chính)

Dự án được tổ chức theo kiến trúc Multi-module Maven để tách biệt rõ ràng các thành phần:

```text
AuctionSystem/
│
├── shared/           # Chứa các Model, hằng số và Protocol giao tiếp chung
│   └── src/main/java/com/auction/shared/
│
├── server/           # Logic máy chủ, DAO, xử lý đa luồng và kết nối CSDL
│   └── src/main/java/com/auction/server/
│
├── client/           # Giao diện người dùng JavaFX và logic kết nối Socket tới Server
│   └── src/main/java/com/auction/client/
│
└── pom.xml           # Root POM quản lý các module và thư viện chung
```

## 4. Vị trí các file .jar

Sau khi build dự án thành công, các file thực thi `.jar` (đã bao gồm toàn bộ thư viện phụ thuộc - fat jar) sẽ nằm tại:

- **Server Executable:** 
  `server/target/server-1.0-SNAPSHOT-jar-with-dependencies.jar`
- **Client Executable:** 
  `client/target/client-1.0-SNAPSHOT-jar-with-dependencies.jar`

## 5. Hướng dẫn chạy Server / Client theo thứ tự cụ thể

**Bước 1: Build toàn bộ dự án**
Mở terminal/command prompt tại thư mục gốc của dự án (`AuctionSystem`) và chạy lệnh Maven:
```bash
mvn clean install -DskipTests
```

**Bước 2: Chạy Server**
- **Quan trọng:** Server phải được khởi chạy ĐẦU TIÊN để lắng nghe các kết nối từ Client.
- Chạy bằng file jar:
  ```bash
  java -jar server/target/server-1.0-SNAPSHOT-jar-with-dependencies.jar
  ```
- *Lưu ý: Server sẽ tự động khởi tạo các bảng trong Database và tạo tài khoản Admin mặc định (`admin` / `admin123`) ở lần chạy đầu tiên.*

**Bước 3: Chạy Client**
- Sau khi Server đã hiển thị thông báo sẵn sàng, bạn có thể khởi chạy một hoặc NHIỀU Client trên các cửa sổ terminal khác nhau.
- Chạy bằng file jar:
  ```bash
  java -jar client/target/client-1.0-SNAPSHOT-jar-with-dependencies.jar
  ```
- *Để test tính năng đa luồng, bạn có thể copy file `client.jar` sang nhiều máy khác nhau trong cùng mạng LAN và chạy đồng thời.*

## 6. Danh sách chức năng đã hoàn thành

**Dành cho Người dùng (Bidder & Seller):**
- Đăng ký / Đăng nhập / Đăng xuất tài khoản.
- Xem danh sách các sản phẩm đang được đấu giá.
- Thêm sản phẩm mới lên sàn đấu giá (Seller).
- Nạp tiền vào tài khoản (với cơ chế trừ tiền cọc - Bid Hold).
- **Đặt giá (Place Bid):** Kiểm tra tính hợp lệ của số dư, số tiền đặt và xử lý đa luồng an toàn.
- **Tự động đặt giá (Auto Bid):** Hệ thống tự động thay mặt người dùng nâng giá từng bước khi bị người khác vượt mặt (đến một giới hạn cho phép).
- Xem lịch sử đặt giá và kết quả chi tiết của phiên đấu giá.
- Nhận thông báo thời gian thực khi có người trả giá cao hơn, hoặc khi kết thúc phiên.
- Xem danh sách các sản phẩm đã thắng thầu (Purchased Products).

**Dành cho Quản trị viên (Admin):**
- Đăng nhập với quyền Admin hệ thống.
- Khóa (Ban) và Mở khóa (Unban) tài khoản người dùng vi phạm.
- Quản lý và xóa các sản phẩm không hợp lệ.
- Xem toàn bộ lịch sử đấu giá trên toàn hệ thống.

**Hệ thống (Core Server):**
- Xử lý đồng thời hàng nghìn kết nối qua Socket.
- Quản lý kết nối Database tối ưu thông qua HikariCP Connection Pool.
- Tự động đóng phiên đấu giá khi hết thời gian và xác định người thắng cuộc.