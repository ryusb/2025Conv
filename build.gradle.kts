plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("mysql:mysql-connector-java:8.0.33")
    // Lombok 라이브러리 추가 (컴파일 시점에만 필요)
    compileOnly("org.projectlombok:lombok:1.18.30")

    implementation("org.apache.commons:commons-dbcp2:2.9.0")
    // Lombok 코드가 실제 Java 코드를 생성하도록 처리 (필수)
    annotationProcessor("org.projectlombok:lombok:1.18.30")
}

tasks.test {
    useJUnitPlatform()
}