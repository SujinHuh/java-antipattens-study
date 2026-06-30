package com.example.order;

// @RestController
// @RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /*
     * 기존 코드:
     *
     * // @PostMapping("/cancel")
     * public Order cancelOrder(/* @RequestBody *\/ OrderCancelRequest request) {
     *     return orderService.cancel(request);
     * }
     *
     * 문제:
     * - Controller가 Order 엔티티를 그대로 반환한다.
     * - API 응답 스펙이 DB/도메인 구조에 강하게 결합된다.
     * - 성공/실패 HTTP 상태코드 기준이 코드에 드러나지 않는다.
     */

    /*
     * 리팩토링 코드:
     * - Service 결과인 Order를 별도 응답 전용 DTO로 변환한다.
     * - 실제 Spring 코드라면 ResponseEntity<OrderCancelResponse>로 감싸
     *   200/204 같은 성공 상태코드를 명확히 표현할 수 있다.
     */
    // @PostMapping("/cancel")
    public OrderCancelResponse cancelOrder(/* @RequestBody */ OrderCancelRequest request) {
        Order order = orderService.cancel(request);
        return OrderCancelResponse.from(order);
    }
}
