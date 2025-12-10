package persistence.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO implements DTO {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String loginId;
    private String password;
    private String userType; // '학생', '교직원', '관리자'

    public UserDTO() {}
}