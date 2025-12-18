package world.usan.usan.dto;

public record LoginViewModel(
        boolean authenticated,
        Long userId,
        String email,
        String nickname,
        String nearbyAddr
) {

    //미로그인 상태
    public static LoginViewModel anonymous() {
        return new LoginViewModel(false, null, null, null, null);
    }

    //로그인 상태
    public static LoginViewModel authenticated(Long userId, String email, String nickname, String nearbyAddr) {
        return new LoginViewModel(true, userId, email, nickname,  nearbyAddr);
    }
}
