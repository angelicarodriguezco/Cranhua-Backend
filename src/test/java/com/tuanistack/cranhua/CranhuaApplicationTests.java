package com.tuanistack.cranhua;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(BasePostgis.class)
class CranhuaApplicationTests {

  @Test
  void contextLoads() {}
}
