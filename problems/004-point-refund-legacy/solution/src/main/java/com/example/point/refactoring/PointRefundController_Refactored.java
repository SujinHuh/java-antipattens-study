package com.example.point.refactoring;

import java.util.List;

// @RestController
// @RequestMapping("/point-refunds")
public class PointRefundController_Refactored {

    private final PointRefundService_Refactored pointRefundService;

    public PointRefundController_Refactored(PointRefundService_Refactored pointRefundService) {
        // 기존 코드: Controller가 new PointRefundService()로 직접 생성했다.
        // 수정 코드: 생성자 주입으로 Spring Bean을 주입받는 형태로 바꾼다.
        this.pointRefundService = pointRefundService;
    }

    // @PostMapping
    public PointRefundResponse_Refactored refund(/* @Valid @RequestBody */ PointRefundRequest_Refactored request) {
        // 기존 코드: PointPayment를 Request와 Response로 직접 사용했다.
        // 수정 코드: 요청/응답 DTO를 분리해 API 계약과 내부 모델을 분리한다.
        return pointRefundService.refund(request);
    }

    // @GetMapping("/history")
    public List<PointRefundResponse_Refactored> history(/* @RequestParam */ Long userId) {
        // 기존 코드: "200 OK: []" 같은 문자열을 Service/Controller가 직접 만들었다.
        // 수정 코드: Controller는 응답 객체를 반환하고 HTTP 상태는 실제 Spring에서 ResponseEntity로 표현할 수 있다.
        return pointRefundService.getRefundHistory(userId);
    }
}
