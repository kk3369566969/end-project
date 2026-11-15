package com.peerreview.dao;

import com.peerreview.domain.Course;

import java.util.List;
import java.util.Optional;

/** 课程数据访问。 */
public interface CourseDao {

    List<Course> findAll();

    List<Course> findByTeacher(long teacherId);

    Optional<Course> findById(long id);

    long insert(Course course);

    void update(Course course);

    void delete(long id);

    /** 课程编码是否重复（编辑时用 excludeId 排除自身）。 */
    boolean existsCode(String code, Long excludeId);
}
