package fr.mtpilates.nutrition;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodRepository extends JpaRepository<Food, UUID> {

    List<Food> findAllByOrderByNameAsc();
}
