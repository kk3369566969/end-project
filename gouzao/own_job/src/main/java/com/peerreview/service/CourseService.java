package com.peerreview.service;

import com.peerreview.common.AppLogger;
import com.peerreview.common.Text;
import com.peerreview.common.ValidationException;
import com.peerreview.dao.AssignmentDao;
import com.peerreview.dao.CourseDao;
import com.peerreview.domain.Assignment;
import com.peerreview.domain.Course;
import com.peerreview.domain.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程管理：教师维护自己开设的课程，学生可以浏览全部课程。
 */
public final class CourseService {

    private final CourseDao courseDao;
    private final AssignmentDao assignmentDao;
    private final AssignmentService assignmentService;

    public CourseService(CourseDao courseDao, AssignmentDao assignmentDao, AssignmentService assignmentService) {
        this.courseDao = courseDao;
        this.assignmentDao = assignmentDao;
        this.assignmentService = assignmentService;
    }

    /** 按角色返回可见课程：教师仅自己的课程，学生可看到全部课程。 */
    public List<Course> listVisible(User user) {
        return user.isTeacher() ? courseDao.findByTeacher(user.getId()) : courseDao.findAll();
    }

    public List<Course> listAll() {
        return courseDao.findAll();
    }

    public Course require(long courseId) {
        return courseDao.findById(courseId)
                .orElseThrow(() -> new ValidationException("课程不存在或已被删除"));
    }

    public Course create(User teacher, String code, String name, String semester) {
        requireTeacher(teacher);
        Course course = new Course();
        applyInput(course, code, name, semester, null);
        course.setTeacherId(teacher.getId());
        course.setCreatedAt(LocalDateTime.now());
        long id = courseDao.insert(course);
        course.setId(id);
        AppLogger.get().info("创建课程：" + course.getCode() + " " + course.getName());
        return course;
    }

    public void update(User teacher, Course course, String code, String name, String semester) {
        requireTeacher(teacher);
        requireOwnership(teacher, course);
        applyInput(course, code, name, semester, course.getId());
        courseDao.update(course);
        AppLogger.get().info("修改课程：" + course.getCode() + " " + course.getName());
    }

    /** 删除课程会级联删除其下作业、提交、互评任务与互评记录。 */
    public void delete(User teacher, long courseId) {
        requireTeacher(teacher);
        Course course = require(courseId);
        requireOwnership(teacher, course);
        for (Assignment assignment : assignmentDao.findByCourse(courseId)) {
            assignmentService.deleteCascade(assignment.getId());
        }
        courseDao.delete(courseId);
        AppLogger.get().info("删除课程：" + course.getCode() + " " + course.getName());
    }

    private void applyInput(Course course, String code, String name, String semester, Long excludeId) {
        String safeCode = Text.requireText(code, "课程代码", 32);
        String safeName = Text.requireText(name, "课程名称", 128);
        String safeSemester = Text.requireText(semester, "学期", 32);
        if (courseDao.existsCode(safeCode, excludeId)) {
            throw new ValidationException("课程代码 " + safeCode + " 已存在，请更换");
        }
        course.setCode(safeCode);
        course.setName(safeName);
        course.setSemester(safeSemester);
    }

    private void requireTeacher(User user) {
        if (!user.isTeacher()) {
            throw new ValidationException("只有教师可以维护课程");
        }
    }

    private void requireOwnership(User teacher, Course course) {
        if (course.getTeacherId() != teacher.getId()) {
            throw new ValidationException("只能维护本人开设的课程");
        }
    }
}
