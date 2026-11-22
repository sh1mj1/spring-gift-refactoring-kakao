package gift.member;

import gift.auth.JwtProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public MemberService(MemberRepository memberRepository, JwtProvider jwtProvider) {
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
    }

    public String register(MemberRequest request) {
        Member member = create(request.email(), request.password());
        if (member == null) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        return jwtProvider.createToken(member.getEmail());
    }

    public String login(MemberRequest request) {
        final Member member = memberRepository.findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (member.getPassword() == null || !member.getPassword().equals(request.password())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        return jwtProvider.createToken(member.getEmail());
    }

    public Member create(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            return null;
        }
        return memberRepository.save(new Member(email, password));
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Member getById(Long id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Member not found. id=" + id));
    }

    public void update(Long id, String email, String password) {
        Member member = getById(id);
        member.update(email, password);
        memberRepository.save(member);
    }

    public void chargePoint(Long id, int amount) {
        Member member = getById(id);
        member.chargePoint(amount);
        memberRepository.save(member);
    }

    public void delete(Long id) {
        memberRepository.deleteById(id);
    }

    public String loginWithKakao(String email, String kakaoAccessToken) {
        Member member = memberRepository.findByEmail(email)
            .orElseGet(() -> new Member(email));
        member.updateKakaoAccessToken(kakaoAccessToken);
        memberRepository.save(member);

        return jwtProvider.createToken(member.getEmail());
    }
}
