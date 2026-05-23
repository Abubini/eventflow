package com.ctbe.eventflow.repository;
import com.ctbe.eventflow.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ScheduleRepository extends JpaRepository<Schedule,Long> {
    List<Schedule> findByEventId(Long eventId);
}
