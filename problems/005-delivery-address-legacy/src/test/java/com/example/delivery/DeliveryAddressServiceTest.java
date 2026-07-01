package com.example.delivery;

public class DeliveryAddressServiceTest {

    // 실제 JUnit 5 환경에서는 `@Test`와 `org.junit.jupiter.api.Assertions.assertThrows`를 사용한다.
    // 이 문제 폴더는 외부 의존성 없이 읽히도록 어노테이션을 주석으로 남긴다.
    // @Test
    public void changeAddressRejectsBlockedAddress() {
        DeliveryAddressRepository repository = new DeliveryAddressRepository();
        DeliveryAddressService service = new DeliveryAddressService(
            repository,
            new BlockedAddressPolicy(),
            new DeliveryAuditLogger()
        );

        // 기존 문제: public field DTO를 만들고, 테스트는 service 호출만 하고 assertion이 없다.
        // DeliveryAddressRequest request = new DeliveryAddressRequest();
        // request.orderId = 100L;
        // request.userId = 1L;
        // request.zipCode = "06234";
        // request.address = "Seoul Seocho";
        // request.memo = "home";
        // service.changeAddress(request);

        DeliveryAddressRequest request = new DeliveryAddressRequest(
            100L,
            1L,
            "06234",
            "TEST address",
            "home"
        );

        assertThrows(DeliveryAddressException.class, () -> service.changeAddress(request));
    }

    private void assertThrows(Class<? extends RuntimeException> expectedType, Runnable executable) {
        try {
            executable.run();
        } catch (RuntimeException e) {
            if (expectedType.isInstance(e)) {
                return;
            }
            throw new AssertionError("unexpected exception type: " + e.getClass().getName(), e);
        }

        throw new AssertionError("expected exception: " + expectedType.getName());
    }
}
