package ru.job4j.dreamjob.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.multipart.MultipartFile;
import ru.job4j.dreamjob.dto.FileDto;
import ru.job4j.dreamjob.model.City;
import ru.job4j.dreamjob.model.Vacancy;
import ru.job4j.dreamjob.service.CityService;
import ru.job4j.dreamjob.service.VacancyService;

import java.util.List;
import java.util.Optional;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;


public class VacancyControllerTest {

    private VacancyService vacancyService;

    private CityService cityService;

    private VacancyController vacancyController;

    private MultipartFile testFile;

    @BeforeEach
    public void initServices() {
        vacancyService = mock(VacancyService.class);
        cityService = mock(CityService.class);
        vacancyController = new VacancyController(vacancyService, cityService);
        testFile = new MockMultipartFile("testFile.img", new byte[] {1, 2, 3});
    }

    /**
     * Тест на метод getAll().
     * Возвращаем список вакансий сохраненных в БД.
     */
    @Test
    public void whenRequestVacancyListPageThenGetPageWithVacancies() {
        var vacancy1 = new Vacancy(1, "test1", "desc1", now(), true, 1, 2);
        var vacancy2 = new Vacancy(2, "test2", "desc2", now(), false, 3, 4);
        var expectedVacancies = List.of(vacancy1, vacancy2);
        when(vacancyService.findAll()).thenReturn(expectedVacancies);

        var model = new ConcurrentModel();
        var view = vacancyController.getAll(model);
        var actualVacancies = model.getAttribute("vacancies");

        assertThat(view).isEqualTo("vacancies/list");
        assertThat(actualVacancies).isEqualTo(expectedVacancies);
    }

    /**
     * Тест на метод getCreationPage().
     * Возвращаем страницу создания вакансий, вместе со списком городов.
     */
    @Test
    public void whenRequestVacancyCreationPageThenGetPageWithCities() {
        var city1 = new City(1, "Москва");
        var city2 = new City(2, "Санкт-Петербург");
        var expectedCities = List.of(city1, city2);
        when(cityService.findAll()).thenReturn(expectedCities);

        var model = new ConcurrentModel();
        var view = vacancyController.getCreationPage(model);
        var actualVacancies = model.getAttribute("cities");

        assertThat(view).isEqualTo("vacancies/create");
        assertThat(actualVacancies).isEqualTo(expectedCities);
    }

    /**
     * Тест на метод create().
     * Сохраняем вакансию и переходим на страницу vacancies.
     */
    @Test
    public void whenSaveVacancyWithFileThenSameDataAndRedirectToVacanciesPage() throws Exception {
        var vacancy = new Vacancy(1, "test1", "desc1", now(), true, 1, 2);
        var fileDto = new FileDto(testFile.getOriginalFilename(), testFile.getBytes());
        var vacancyArgumentCaptor = ArgumentCaptor.forClass(Vacancy.class);
        var fileDtoArgumentCaptor = ArgumentCaptor.forClass(FileDto.class);
        when(vacancyService.save(vacancyArgumentCaptor.capture(), fileDtoArgumentCaptor.capture()))
                .thenReturn(vacancy);

        var model = new ConcurrentModel();
        var view = vacancyController.create(vacancy, testFile, model);
        var actualVacancy = vacancyArgumentCaptor.getValue();
        var actualFileDto = fileDtoArgumentCaptor.getValue();

        assertThat(view).isEqualTo("redirect:/vacancies");
        assertThat(actualVacancy).isEqualTo(vacancy);
        assertThat(fileDto).usingRecursiveComparison().isEqualTo(actualFileDto);

    }

    /**
     * Тест на метод create().
     * Получаем ошибку при попытке сохранить вакансию с файлом.
     * Переходим на страницу 404 с сообщением об ошибке.
     */
    @Test
    public void whenSomeExceptionThrownThenGetErrorPageWithMessage() {
        var expectedException = new RuntimeException("Failed to write file");
        when(vacancyService.save(any(), any())).thenThrow(expectedException);

        var model = new ConcurrentModel();
        var view = vacancyController.create(new Vacancy(), testFile, model);
        var actualExceptionMessage = model.getAttribute("message");

        assertThat(view).isEqualTo("errors/404");
        assertThat(actualExceptionMessage).isEqualTo(expectedException.getMessage());
    }

    /**
     * Тест на метод getById() - нахождение вакансии по id и возврат ее страницы.
     */
    @Test
    public void whenRequestVacancyByIdThenGetItPage() {
        var vacancy = new Vacancy(1, "test1", "desc1", now(), true, 1, 2);
        var expectedVacancy = Optional.of(vacancy);
        when(vacancyService.findById(1)).thenReturn(expectedVacancy);

        var model = new ConcurrentModel();
        var view = vacancyController.getById(model, vacancy.getId());
        var actualVacancy = model.getAttribute("vacancy");

        assertThat(view).isEqualTo("vacancies/one");
        assertThat(actualVacancy).isEqualTo(expectedVacancy.get());
    }

    /**
     * Тест на метод getById(). Поиск по-несуществующему id
     * ведет к странице 404 с сообщением об ошибке.
     */
    @Test
    public void whenRequestNotExistingVacancyByIdThenGetGetErrorPageWithMessage() {
        var expectedException = new RuntimeException(
                "Вакансия с указанным идентификатором не найдена");
        when(vacancyService.findById(0)).thenReturn(Optional.empty());

        var model = new ConcurrentModel();
        var view = vacancyController.getById(model, 0);
        var actualExceptionMessage = model.getAttribute("message");

        assertThat(view).isEqualTo("errors/404");
        assertThat(actualExceptionMessage).isEqualTo(expectedException.getMessage());
    }

    /**
     * Тест метода update(). При успешном обновлении вакансии возвращает true.
     */
    @Test
    public void whenUpdateVacancyThenGetUpdatedVacancy() {
        var updatedVacancy = new Vacancy(1, "test1Updated", "desc1Updated", now(), true, 1, 2);
        var vacancyArgumentCaptor = ArgumentCaptor.forClass(Vacancy.class);
        var fileDtoArgumentCaptor = ArgumentCaptor.forClass(FileDto.class);
        when(vacancyService.update(vacancyArgumentCaptor.capture(),
                fileDtoArgumentCaptor.capture())).thenReturn(true);

        var model = new ConcurrentModel();
        var view = vacancyController.update(updatedVacancy, testFile, model);
        var actualVacancy = vacancyArgumentCaptor.getValue();

        assertThat(view).isEqualTo("redirect:/vacancies");
        assertThat(actualVacancy).isEqualTo(updatedVacancy);
    }

    /**
     * Тест на метод update().
     * Обновление несуществующей вакансии ведет к странице 404 с сообщением об ошибке.
     */
    @Test
    public void whenUpdateNotExistingVacancyThenGetErrorPageWithMessage() {
        var expectedException = new RuntimeException("Вакансия не найдена");
        when(vacancyService.update(any(), any())).thenThrow(expectedException);

        var model = new ConcurrentModel();
        var view = vacancyController.update(new Vacancy(), testFile, model);
        var actualException = model.getAttribute("message");

        assertThat(view).isEqualTo("errors/404");
        assertThat(actualException).isEqualTo(expectedException.getMessage());
    }

    /**
     * Тест на метод delete()
     * Успешное удаление вакансии. Переход на страницу vacancies.
     */
    @Test
    public void whenDeleteVacancyByIdThenTrue() {
        var id = 1;
        var isDeleted = true;
        when(vacancyService.deleteById(id)).thenReturn(isDeleted);

        var model = new ConcurrentModel();
        var view = vacancyController.delete(model, id);
        var actualDeleted = vacancyService.deleteById(id);

        assertThat(view).isEqualTo("redirect:/vacancies");
        assertThat(actualDeleted).isEqualTo(isDeleted);
    }

    /**
     * Тест на метод delete()
     * Удаление несуществующей вакансии ведет к странице 404 с сообщением об ошибке.
     */
    @Test
    public void whenDeleteVacancyByIdThenGetErrorPageWithMessage() {
        var expectedException = new RuntimeException(
                "Вакансия с указанным идентификатором не найдена");
        var id = 1;
        var isDeleted = false;
        when(vacancyService.deleteById(id)).thenReturn(isDeleted);

        var model = new ConcurrentModel();
        var view = vacancyController.delete(model, id);
        var actualExceptionMessage = model.getAttribute("message");

        assertThat(view).isEqualTo("errors/404");
        assertThat(actualExceptionMessage).isEqualTo(expectedException.getMessage());
    }
}