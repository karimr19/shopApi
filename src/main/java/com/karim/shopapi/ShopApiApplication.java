package com.karim.shopapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

/**
 * Spring boot приложение.
 */
@SpringBootApplication
public class ShopApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopApiApplication.class, args);
    }

//    @Bean
//    CommandLineRunner runner(ShopUnitRepository repository) {
//        return args -> {
//            String name = "lego";
//            ShopUnit first = new ShopUnit(
//                    "dog",
//                    LocalDateTime.now(),
//                    null,
//                    ShopUnitType.OFFER,
//                    5,
//                    null
//                    );
//            ShopUnit second = new ShopUnit(
//                    "dog",
//                    LocalDateTime.now(),
//                    null,
//                    ShopUnitType.OFFER,
//                    5,
//                    null
//            );
//            ShopUnit shopUnit = new ShopUnit(
//                    "cat",
//                    LocalDateTime.now(),
//                    null,
//                    ShopUnitType.CATEGORY,
//                    5,
//                    List.of(first, second)
//            );
//            repository.insert(shopUnit);
////            repository.findShopUnitByName(name)
////                    .ifPresentOrElse(s -> {
////                        System.out.println(s + " already exists");
////                    }, () -> {
////                        System.out.println("Inserting shop unit " + shopUnit);
////                        repository.insert(shopUnit);
////                    });
//        };
//    }
}
