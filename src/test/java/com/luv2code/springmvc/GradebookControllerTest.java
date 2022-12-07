package com.luv2code.springmvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luv2code.springmvc.models.CollegeStudent;
import com.luv2code.springmvc.repository.HistoryGradesDao;
import com.luv2code.springmvc.repository.MathGradesDao;
import com.luv2code.springmvc.repository.ScienceGradesDao;
import com.luv2code.springmvc.repository.StudentDao;
import com.luv2code.springmvc.service.StudentAndGradeService;
import org.jboss.jandex.JandexAntTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestPropertySource("/application-test.properties")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional  // we will use jpa entity manager directly in this teset
public class GradebookControllerTest {

    private static MockHttpServletRequest request;

    @PersistenceContext
    private EntityManager entityManager;

    @Mock
    StudentAndGradeService studentCreateServiceMock;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StudentDao studentDao;

    @Autowired
    private MathGradesDao mathGradeDao;

    @Autowired
    private ScienceGradesDao scienceGradeDao;

    @Autowired
    private HistoryGradesDao historyGradeDao;

    @Autowired
    private StudentAndGradeService studentService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private CollegeStudent student;

    @Value("${sql.script.create.student}")
    private String sqlAddStudent;

    @Value("${sql.script.create.math.grade}")
    private String sqlAddMathGrade;

    @Value("${sql.script.create.science.grade}")
    private String sqlAddScienceGrade;

    @Value("${sql.script.create.history.grade}")
    private String sqlAddHistoryGrade;

    @Value("${sql.script.delete.student}")
    private String sqlDeleteStudent;

    @Value("${sql.script.delete.math.grade}")
    private String sqlDeleteMathGrade;

    @Value("${sql.script.delete.science.grade}")
    private String sqlDeleteScienceGrade;

    @Value("${sql.script.delete.history.grade}")
    private String sqlDeleteHistoryGrade;

    public static final MediaType APPLICATION_JSON_UTF8 = MediaType.APPLICATION_JSON;

    @BeforeAll
    public static void setup() {
        request = new MockHttpServletRequest();

        request.setParameter("firstname", "Rick");
        request.setParameter("lastname", "Norman");
        request.setParameter("emailAddress", "rick.norman@luv2code.com");
    }

    @BeforeEach
    public void setupDatabase() {
        jdbc.execute(sqlAddStudent);
        jdbc.execute(sqlAddMathGrade);
        jdbc.execute(sqlAddScienceGrade);
        jdbc.execute(sqlAddHistoryGrade);
    }

    @AfterEach
    public void setupAfterTransaction() {
        jdbc.execute(sqlDeleteStudent);
        jdbc.execute(sqlDeleteMathGrade);
        jdbc.execute(sqlDeleteScienceGrade);
        jdbc.execute(sqlDeleteHistoryGrade);
    }

    @Test
    public void getStudentHttpRequest() throws Exception {

        student.setFirstname("Chad");
        student.setLastname("Darby");
        student.setEmailAddress("chad.darby@luv2code.com");
//        They are both the same:
        studentDao.save(student);
//        entityManager.persist(student);
//        entityManager.flush();

        mockMvc.perform(MockMvcRequestBuilders.get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(2)));

    }

    @Test
    public void createStudentHttpRequest() throws Exception {

        student.setFirstname("Chad");
        student.setLastname("Darby");
        student.setEmailAddress("chad.darby@luv2code.com");

        // send the post request
        mockMvc.perform(MockMvcRequestBuilders.post("/")
                        .contentType(APPLICATION_JSON_UTF8)
                        .content(objectMapper.writeValueAsString(student)))
                // verify status, content type
                .andExpect(status().isOk())
                // verify number of student after request
                .andExpect(jsonPath("$", hasSize(2)));

        // verify the backend
        CollegeStudent verifyStudent = studentDao.findByEmailAddress("chad.darby@luv2code.com");
        assertNotNull(verifyStudent);

    }

    @Test
    public void deleteStudentHttpRequest() throws Exception {

        // verify student exist before
        assertTrue(studentDao.findById(1).isPresent());

        // send the delete request
        mockMvc.perform(MockMvcRequestBuilders.delete("/student/{id}", 1))
                // verify status, content type
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                // verify number of students after request
                .andExpect(jsonPath("$", hasSize(0)));
        // verify backend
        assertFalse(studentDao.findById(1).isPresent());
    }

    @Test
    public void deleteStudentHttpRequestStudentDoesNotExist() throws Exception {

        // verify student does not exist before
        assertTrue(studentDao.findById(100).isEmpty());

        // send the delete request
        mockMvc.perform(MockMvcRequestBuilders.delete("/student/{id}", 100))
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }

    @Test
    public void getStudentInformation() throws Exception {

        // verify student exist
        assertTrue(studentDao.findById(1).isPresent());

        // send the request
        mockMvc.perform(MockMvcRequestBuilders.get("/studentInformation/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.firstname", is("Eric")))
                .andExpect(jsonPath("$.lastname", is("Roby")))
                .andExpect(jsonPath("$.emailAddress", is("eric.roby@luv2code_school.com")))
                .andExpect(jsonPath("$.studentGrades.mathGradeResults", hasSize(1)))
                .andExpect(jsonPath("$.studentGrades.historyGradeResults", hasSize(1)))
                .andExpect(jsonPath("$.studentGrades.scienceGradeResults", hasSize(1)));
    }

    @Test
    public void getStudentInformationHttpRequestStudentDoesNotExist() throws Exception {

        // verify student does not exist before
        assertTrue(studentDao.findById(100).isEmpty());

        // send the request
        mockMvc.perform(MockMvcRequestBuilders.get("/studentInformation/{id}", 100))
                // verify 4xx status
                .andExpect(status().is4xxClientError())
                // content type : json
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                // expect status is 404 in response
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }

    @Test
    public void createGradeHttpRequest() throws Exception {

        // verify student exist
        assertTrue(studentDao.findById(1).isPresent());

        // make request
        mockMvc.perform(MockMvcRequestBuilders.post("/grades")
                .contentType(APPLICATION_JSON_UTF8)
                .param("grade", "85.00")
                .param("gradeType", "math")
                .param("studentId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.firstname", is("Eric")))
                .andExpect(jsonPath("$.lastname", is("Roby")))
                .andExpect(jsonPath("$.emailAddress", is("eric.roby@luv2code_school.com")))
                .andExpect(jsonPath("$.studentGrades.mathGradeResults", hasSize(2)))
                .andExpect(jsonPath("$.studentGrades.historyGradeResults", hasSize(1)))
                .andExpect(jsonPath("$.studentGrades.scienceGradeResults", hasSize(1)));

    }

    @Test
    public void createGradeHttpRequestStudentDoesNotExist() throws Exception {

        // verify student does not exist before
        assertTrue(studentDao.findById(100).isEmpty());
        mockMvc.perform(MockMvcRequestBuilders.post("/grades")
                        .contentType(APPLICATION_JSON_UTF8)
                        .param("grade", "85.00")
                        .param("gradeType", "math")
                        .param("studentId", "100"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }

    @Test
    public void createGradeHttpRequestInvalidGradeType() throws Exception {

        // verify student exist before
        assertTrue(studentDao.findById(1).isPresent());

        mockMvc.perform(MockMvcRequestBuilders.post("/grades")
                        .contentType(APPLICATION_JSON_UTF8)
                        .param("grade", "85.00")
                        .param("gradeType", "literature")
                        .param("studentId", "100"))
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }

    @Test
    public void deleteGradeHttpRequest() throws Exception {

        // verify grade exist
        assertTrue(mathGradeDao.findById(1).isPresent());

        mockMvc.perform(MockMvcRequestBuilders.delete("/grades/{id}/{gradeType}", 1, "math"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.firstname", is("Eric")))
                .andExpect(jsonPath("$.lastname", is("Roby")))
                .andExpect(jsonPath("$.emailAddress", is("eric.roby@luv2code_school.com")))
                .andExpect(jsonPath("$.studentGrades.mathGradeResults", hasSize(0)))
                .andExpect(jsonPath("$.studentGrades.historyGradeResults", hasSize(1)))
                .andExpect(jsonPath("$.studentGrades.scienceGradeResults", hasSize(1)));
    }

    @Test
    public void deleteGradeHttpRequestGradeIdDoesNotExist() throws Exception {

        // verify grade does not exist
        assertTrue(mathGradeDao.findById(100).isEmpty());
        mockMvc.perform(MockMvcRequestBuilders.delete("/grades/{id}/{gradeType}", 100, "math"))
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }

    @Test
    public void deleteGradeHttpRequestInvalidGradeType() throws Exception {


        mockMvc.perform(MockMvcRequestBuilders.delete("/grades/{id}/{gradeType}", 100, "literature"))
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Student or Grade was not found")));
    }
}
