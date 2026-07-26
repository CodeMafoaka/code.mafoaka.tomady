# SOUSPAPE Architectures and Guidelines

## Project Structure
This is a standard production-ready Spring Boot 3.x backend application written in Java 17. The design aligns on Service-Oriented Domain Layer principles.

## Module Layout
- `com.tomady.nutrition.dto`: Standard Java Records representing API Transfer Objects (UserProfile, BioRecord, Dish, FoodItem, NutrientProperty, etc.).
- `com.tomady.nutrition.service`: Interface-driven Domain Services mapping core logic (`FooDbService`, `DietService`, `GemmaLlmService`, `DailySuggestionService`).
- `com.tomady.nutrition.controller`: REST Web layer exposing Swagger annotated endpoints at standard URIs.
- `com.tomady.nutrition.exception`: RFC 7807 compliant Problem Detail format handlers.

## Integration & Verification
To test compile and execute all mock-MVC integration suites:
`mvn clean test`
