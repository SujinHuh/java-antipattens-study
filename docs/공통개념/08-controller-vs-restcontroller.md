# `@Controller` vs `@RestController`

Spring MVC에서 클라이언트의 HTTP 요청을 처리하는 컨트롤러 클래스에 붙이는 애노테이션입니다. 두 애노테이션의 가장 큰 차이점은 **HTTP 응답(Response)에 무엇을 담아서 반환하느냐**입니다.

---

## 1. `@Controller` (전통적인 Spring MVC)

주로 **HTML 화면(View)을 반환**할 때 사용합니다. 
메서드가 문자열(String)을 반환하면, Spring은 그 문자열을 "HTML 파일의 이름"으로 인식하고 해당 파일을 찾아서 클라이언트(브라우저)에게 보여줍니다.

```java
@Controller
public class WebController {

    @GetMapping("/home")
    public String home() {
        // "home" 이라는 이름의 HTML 파일(예: home.html)을 렌더링해서 반환함
        return "home"; 
    }
}
```

* **데이터(JSON)를 반환하고 싶다면?**
  `@Controller` 안에서 HTML 뷰가 아니라 순수한 데이터를 반환하고 싶다면, 해당 메서드에 **`@ResponseBody`** 라는 애노테이션을 추가로 붙여야 합니다.

```java
@Controller
public class WebController {

    @GetMapping("/api/data")
    @ResponseBody // HTML 파일 이름이 아니라, 이 데이터 자체를 응답 Body에 넣어라!
    public UserData getData() {
        return new UserData("sujin", 25); // JSON으로 변환되어 클라이언트에게 전달됨
    }
}
```

---

## 2. `@RestController` (REST API용)

주로 JSON 형식의 **순수 데이터(API)를 반환**할 때 사용합니다.
모던 웹 개발(React, Vue, 모바일 앱 등)에서는 서버가 화면(HTML)을 직접 그리지 않고 데이터(JSON)만 넘겨주는 방식을 많이 쓰는데, 이때 사용합니다.

사실 `@RestController` 안을 열어보면 아래와 같이 생겼습니다.
```java
@Controller
@ResponseBody
public @interface RestController { ... }
```
즉, **`@RestController = @Controller + @ResponseBody`** 입니다.

```java
@RestController // 클래스 전체에 @ResponseBody가 기본으로 적용됨
public class ApiController {

    @GetMapping("/api/users")
    public UserData getUser() {
        // @ResponseBody를 일일이 안 붙여도, 자동으로 객체가 JSON으로 변환되어 응답됨!
        return new UserData("sujin", 25); 
    }
}
```

---

## 3. 핵심 요약 비교

| 특징 | `@Controller` | `@RestController` |
|------|--------------|-------------------|
| **주 목적** | View(HTML 화면) 반환 | Data(JSON/XML) 반환 (REST API) |
| **동작 방식** | `ViewResolver`가 작동하여 HTML 파일을 찾음 | `MessageConverter`가 작동하여 객체를 JSON으로 직렬화 |
| **데이터 반환 시** | 메서드에 `@ResponseBody`를 붙여야 함 | 기본적으로 모든 메서드에 `@ResponseBody`가 적용되어 있음 |

> **실무 팁:**
> 요즘 대부분의 백엔드 서버는 브라우저 화면(HTML)을 직접 서빙하기보다는, 프론트엔드(React/Vue)나 모바일 앱과 통신하기 위한 **REST API** 역할을 합니다. 따라서 십중팔구 **`@RestController`** 를 기본으로 사용한다고 생각하시면 됩니다!
