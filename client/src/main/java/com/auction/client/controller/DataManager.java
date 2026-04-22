package Part1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class


DataManager {
    public static ObservableList<Product> sharedProductList = FXCollections.observableArrayList();
    public static ObservableList<User> allUsers = FXCollections.observableArrayList();
    /*Tạo biến tài khoản chung cho app ,dùng public để ở đâu cũng có thể dùng đuợc và dùng static để nó trở thành
    biến của lớp chứ ko phải của riêng đối tượng nào nên khi cần thêm tài khoản vào chỉ cần allUsers.add là được
    Phải tạo ObservableList vì khi sửa hay thêm, nó sẽ báo cho bảng trên màn hình tự động cập nhật,khác với List thì nó
    phải thêm dòng code để tạo mới giao diện
    */
    static {
        allUsers.add(new User("admin", "Quản trị viên", "admin@system.com", "Admin123", "Admin"));
    }
    //Tạo một tài khoản admin mà chỉ có một người admin biết và các tài khoản khác tạo sẽ không thể tạo giống tài khoản này được
}