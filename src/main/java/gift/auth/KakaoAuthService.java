package gift.auth;

import gift.member.MemberService;
import org.springframework.stereotype.Service;

@Service
public class KakaoAuthService {
    private final KakaoLoginClient kakaoLoginClient;
    private final MemberService memberService;

    public KakaoAuthService(KakaoLoginClient kakaoLoginClient, MemberService memberService) {
        this.kakaoLoginClient = kakaoLoginClient;
        this.memberService = memberService;
    }

    public String login(String code) {
        KakaoLoginClient.KakaoTokenResponse kakaoTokenResponse = kakaoLoginClient.requestAccessToken(code);
        String kakaoAccessToken = kakaoTokenResponse.accessToken();
        KakaoLoginClient.KakaoUserResponse kakaoUser = kakaoLoginClient.requestUserInfo(kakaoAccessToken);
        String email = kakaoUser.email();

        return memberService.loginWithKakao(email, kakaoAccessToken);
    }
}
