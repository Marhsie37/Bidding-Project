Người A – Frontend (JavaFX Developer)
Trách nhiệm chính:

Xây dựng toàn bộ giao diện người dùng (UI) và điều khiển luồng màn hình (UX).

Kết nối giao diện với các sự kiện từ người dùng (đăng nhập, đăng ký, đặt giá, xem danh sách sản phẩm…).

Công việc cụ thể:

Thiết kế các màn hình chính: đăng nhập, đăng ký, dashboard cho người dùng thường, người bán, admin.

Xây dựng màn hình đấu giá realtime (hiển thị biểu đồ, lịch sử đặt giá, đặt giá thủ công, auto-bid).

Điều phển chuyển màn hình (scene navigation) từ MainApp.java.

Định nghĩa các model client (chỉ để hiển thị dữ liệu lên UI).

Kết quả bàn giao:

Các file .fxml và Controller.java hoàn chỉnh.

Ứng dụng client chạy được, giao tiếp được với server qua socket (phối hợp với Người C).


 Người B – Backend & Database (Java + MySQL)
Trách nhiệm chính:

Thiết kế cơ sở dữ liệu, viết các lớp DAO (CRUD), quản lý dữ liệu người dùng, sản phẩm, phiên đấu giá, lịch sử đặt giá.

Đảm bảo dữ liệu được lưu trữ và truy xuất đúng.

Công việc cụ thể:

Tạo database auction_system và tự động tạo bảng khi chạy DatabaseConnection.

Viết UserDAO, ProductDAO, AuctionDAO, BidDAO cho các thao tác: thêm, sửa, xóa, truy vấn.

Định nghĩa các entity phía server và shared models dùng chung với client.

Kết quả bàn giao:

Database hoạt động ổn định.

Các DAO được gọi từ tầng business logic (Người D) để lưu/đọc dữ liệu.


 Người C – Network & Communication (Socket, Concurrency)
Trách nhiệm chính:

Xây dựng hệ thống giao tiếp mạng giữa client và server qua socket (TCP).

Quản lý đa luồng, đẩy dữ liệu realtime (thông báo giá mới, kết thúc phiên) từ server đến nhiều client.

Triển khai Observer pattern để cập nhật giá theo thời gian thực.

Công việc cụ thể:

Viết AuctionServer (lắng nghe kết nối, thread pool, quản lý client).

Viết ClientHandler xử lý từng kết nối, định tuyến request.

Viết NotificationService quản lý danh sách subscriber và gửi realtime update.

Viết SocketClient phía client để gửi request/nhận response bất đồng bộ.

Định nghĩa giao thức truyền thông: CommandType, Request, Response, JsonUtils.

Kết quả bàn giao:

Client – server giao tiếp được.

Realtime push khi có giá thầu mới hoặc phiên đấu giá kết thúc.

Hỗ trợ anti-sniping ở tầng mạng (gửi tín hiệu mở rộng thời gian).


 Người D – Business Logic & DevOps
Trách nhiệm chính:

Viết toàn bộ logic nghiệp vụ cốt lõi: đặt giá, kiểm tra hợp lệ, xử lý auto-bid, điều khiển kết thúc phiên.

Quản lý cấu hình build (Maven), CI/CD (GitHub Actions), viết unit test.

Đảm bảo xử lý đồng thời đúng đắn (race condition, thread-safe).

Công việc cụ thể:

Viết AuctionService (xử lý đặt giá, kết thúc đấu giá, anti-sniping logic).

Viết AutoBidService (thuật toán auto-bid dùng PriorityQueue, maxBid, increment).

Viết ServerController làm trung gian nhận request từ network (Người C) và gọi xuống service.

Cấu hình pom.xml multi-module (shared, server, client).

Viết unit test cho AuctionService, AutoBidService, DatabaseConnection.

Thiết lập GitHub Actions tự động build, test, deploy.

Kết quả bàn giao:

Hệ thống đấu giá chạy đúng luật (giá tăng dần, auto-bid, chống snipe).

Maven build thành công, CI/CD hoạt động.

Code có test coverage cơ bản.

Vị trí các file jar :
shared/target/shared-1.0-SNAPSHOT.jar
server/target/server-1.0-SNAPSHOT-jar-with-dependencies.jar
client/target/client-1.0-SNAPSHOT-jar-with-dependencies.jar

Hướng dẫn chạy 
Chạy lần lượt 2 dòng sau
java -jar server/target/server-1.0-SNAPSHOT-jar-with-dependencies.jar
java -jar client/target/client-1.0-SNAPSHOT-jar-with-dependencies.jar


 