package by.belakhvostsik;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.Statistics;

import javax.persistence.*;
import java.io.Serializable;

public class HibernateCacheExample implements Serializable {

    @Entity
    @Table(name = "users")
    @Cacheable
    @org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
    public static class User implements Serializable{
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY )
        private Long id;
        private String name;
        private static final long serialVersionUID = 1L;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static void main(String[] args) {
        Configuration config = new Configuration()
                .addAnnotatedClass(User.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.hbm2ddl.auto", "create")
                .setProperty("hibernate.show_sql", "true")
                .setProperty("hibernate.format_sql", "true")
                .setProperty("hibernate.cache.use_second_level_cache", "true")
                .setProperty("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory")
                .setProperty("hibernate.cache.use_query_cache", "true")
                .setProperty("hibernate.generate_statistics", "true");

        SessionFactory sessionFactory = config.buildSessionFactory();
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);

        // Создаем тестовые данные
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            User user = new User();
            user.setName("John");
            session.save(user);
            session.getTransaction().commit();
        }

        // Демонстрация кэша 1 уровня
        System.out.println("\nКэш 1 уровня:");
        try (Session session = sessionFactory.openSession()) {
            System.out.println("Первый запрос:");
            session.get(User.class, 1L);

            System.out.println("Второй запрос:");
            session.get(User.class, 1L); // Без SQL запроса
        }
        sessionFactory.getCache().evictEntityData(User.class);

        // Демонстрация кэша 2 уровня
        System.out.println("\nКэш 2 уровня:");
        try (Session session1 = sessionFactory.openSession()) {
            System.out.println("Первая сессия:");
            session1.get(User.class, 1L);
        }

        try (Session session2 = sessionFactory.openSession()) {
            System.out.println("Вторая сессия:");
            session2.get(User.class, 1L); // Без SQL запроса
        }


        // Статистика
        System.out.println("\nСтатистика:");
        System.out.println("Кэш 2 уровня - попадания: " + stats.getSecondLevelCacheHitCount());
        System.out.println("Кэш 2 уровня - промахи: " + stats.getSecondLevelCacheMissCount());
        sessionFactory.close();
    }
}