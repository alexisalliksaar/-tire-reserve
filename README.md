# tire-reserve

This project integrates the [tire-change-workshop](https://github.com/Surmus/tire-change-workshop) backend API's through a web application written using Java, Spring Boot and Vue 3.

## Running the application
1. Follow the [guide](https://github.com/Surmus/tire-change-workshop?tab=readme-ov-file#usage) of tire-change-workshop on how to run the workshop servers
2. Install Java JDK 21 (JAVA_HOME variable must be set)
3. Run the Maven Wrapper in [backend](backend) directory:
   1) On Windows:
      ```sh
       mvnw.cmd clean package spring-boot:run
       ```
   2) On Unix:
      ```sh
      mvnw clean package spring-boot:run
      ```
4. Wait until the application is ready and then open http://localhost:8080/ in your browser
