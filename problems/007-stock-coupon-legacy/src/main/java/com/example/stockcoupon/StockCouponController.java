package com.example.stockcoupon;

// @RestController
// @RequestMapping("/stock-coupons")
public class StockCouponController {

    private final StockCouponService stockCouponService;

    public StockCouponController(StockCouponService stockCouponService) {
        this.stockCouponService = stockCouponService;
    }

    // @PostMapping("/issue")
    public StockCouponResponse issue(/* @RequestBody */ StockCouponRequest request) {
        if (request == null) {
            return new StockCouponResponse(null, null, "FAILED", 0);
        }

        return stockCouponService.issue(request);
    }
}
