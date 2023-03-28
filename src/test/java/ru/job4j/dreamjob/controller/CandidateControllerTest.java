package ru.job4j.dreamjob.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.multipart.MultipartFile;
import ru.job4j.dreamjob.dto.FileDto;
import ru.job4j.dreamjob.model.Candidate;
import ru.job4j.dreamjob.model.City;
import ru.job4j.dreamjob.service.CandidateService;
import ru.job4j.dreamjob.service.CityService;

import java.util.List;
import java.util.Optional;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CandidateControllerTest {
    private CandidateService candidateService;

    private CityService cityService;

    private CandidateController candidateController;

    private MultipartFile testFile;

    @BeforeEach
    public void initServices() {
        candidateService = mock(CandidateService.class);
        cityService = mock(CityService.class);
        candidateController = new CandidateController(candidateService, cityService);
        testFile = new MockMultipartFile("testFile.img", new byte[] {1, 2, 3});
    }

    /**
     * Тест на метод getAll().
     * Возвращаем список кандидатов сохраненных в БД.
     */
    @Test
    public void whenRequestCandidateListPageThenGetPageWithCandidate() {
        var candidate1 = new Candidate(1, "candidate1", "desc1", now(), 1, 1);
        var candidate2 = new Candidate(2, "candidate2", "desc2", now(), 2, 2);
        var expectedCandidates = List.of(candidate1, candidate2);
        when(candidateService.findAll()).thenReturn(expectedCandidates);

        var model = new ConcurrentModel();
        var view = candidateController.getAll(model);
        var actualCandidate = model.getAttribute("candidates");

        assertThat(view).isEqualTo("candidates/list");
        assertThat(actualCandidate).isEqualTo(expectedCandidates);
    }

    /**
     * Тест на метод getCreationPage().
     * Возвращаем страницу создания кандидатов, вместе со списком городов.
     */
    @Test
    public void whenRequestCandidateCreationPageThenGetPageWithCities() {
        var city1 = new City(1, "Москва");
        var city2 = new City(2, "Санкт-Петербург");
        var expectedCities = List.of(city1, city2);
        when(cityService.findAll()).thenReturn(expectedCities);

        var model = new ConcurrentModel();
        var view = candidateController.getCreationPage(model);
        var actualCandidates = model.getAttribute("cities");

        assertThat(view).isEqualTo("candidates/create");
        assertThat(actualCandidates).isEqualTo(expectedCities);
    }

    /**
     * Тест на метод create().
     * Сохраняем кандидата и переходим на страницу candidates.
     */
    @Test
    public void whenSaveCandidateWithFileThenSameDataAndGetCandidatesPage() throws Exception {
        var candidate = new Candidate(1, "candidate1", "desc1", now(), 1, 1);
        var fileDto = new FileDto(testFile.getOriginalFilename(), testFile.getBytes());
        var candidateArgumentCaptor = ArgumentCaptor.forClass(Candidate.class);
        var fileDtoArgumentCaptor = ArgumentCaptor.forClass(FileDto.class);
        when(candidateService.save(candidateArgumentCaptor.capture(),
                fileDtoArgumentCaptor.capture()))
                .thenReturn(candidate);

        var model = new ConcurrentModel();
        var view = candidateController.create(candidate, testFile, model);
        var actualCandidate = candidateArgumentCaptor.getValue();
        var actualFileDto = fileDtoArgumentCaptor.getValue();

        assertThat(view).isEqualTo("redirect:/candidates");
        assertThat(actualCandidate).isEqualTo(candidate);
        assertThat(fileDto).usingRecursiveComparison().isEqualTo(actualFileDto);
    }

    /**
     * Тест на метод create().
     * Получаем ошибку при попытке сохранить кандидата с файлом.
     * Переходим на страницу 404 с сообщением об ошибке.
     */
    @Test
    public void whenSomeExceptionThrownThenGetErrorPageWithMessage() {
        var expectedException = new RuntimeException("Failed to write file");
        when(candidateService.save(any(), any())).thenThrow(expectedException);

        var model = new ConcurrentModel();
        var view = candidateController.create(new Candidate(), testFile, model);
        var actualExceptionMessage = model.getAttribute("message");

        assertThat(view).isEqualTo("errors/404");
        assertThat(actualExceptionMessage).isEqualTo(expectedException.getMessage());
    }

    /**
     * Тест на метод getById() - нахождение кандидата по id и возврат его страницы.
     */
    @Test
    public void whenRequestCandidateByIdThenGetHisPage() {
        var candidate = new Candidate(1, "candidate1", "desc1", now(), 1, 1);
        var expectedCandidate = Optional.of(candidate);
        when(candidateService.findById(1)).thenReturn(expectedCandidate);

        var model = new ConcurrentModel();
        var view = candidateController.getById(model, candidate.getId());
        var actualCandidate = model.getAttribute("candidate");

        assertThat(view).isEqualTo("candidates/one");
        assertThat(actualCandidate).isEqualTo(expectedCandidate.get());
    }

    /**
     * Тест на метод getById().
     * Поиск кандидата по-несуществующему id
     * ведет на страницу 404 с сообщением об ошибке.
     */
    @Test
    public void whenRequestNotExistingCandidateByIdThenGetGetErrorPageWithMessage() {
        var expectedException = new RuntimeException(
                "Резюме с указанным идентификатором не найдено");
        when(candidateService.findById(0)).thenReturn(Optional.empty());

        var model = new ConcurrentModel();
        var view = candidateController.getById(model, 0);
        var actualExceptionMessage = model.getAttribute("message");

        assertThat(view).isEqualTo("errors/404");
        assertThat(actualExceptionMessage).isEqualTo(expectedException.getMessage());
    }

    /**
     * Тест метода update(). При успешном обновлении кандидата возвращает true.
     */
    @Test
    public void whenUpdateCandidateThenGetUpdatedCandidate() {
        var candidateUpdated = new Candidate(1, "candidate1", "desc1", now(), 1, 1);
        var candidateArgumentCaptor = ArgumentCaptor.forClass(Candidate.class);
        var fileDtoArgumentCaptor = ArgumentCaptor.forClass(FileDto.class);
        when(candidateService.update(candidateArgumentCaptor.capture(),
                fileDtoArgumentCaptor.capture())).thenReturn(true);

        var model = new ConcurrentModel();
        var view = candidateController.update(candidateUpdated, testFile, model);
        var actualCandidate = candidateArgumentCaptor.getValue();

        assertThat(view).isEqualTo("redirect:/candidates");
        assertThat(actualCandidate).isEqualTo(candidateUpdated);
    }

    /**
     * Тест на метод update().
     * Обновление несуществующего кандидата ведет к странице 404 с сообщением об ошибке.
     */
    @Test
    public void whenUpdateNotExistingCandidateThenGetErrorPageWithMessage() {
        var expectedException = new RuntimeException("Резюме не найдено");
        when(candidateService.update(any(), any())).thenThrow(expectedException);

        var model = new ConcurrentModel();
        var view = candidateController.update(new Candidate(), testFile, model);
        var actualExceptionMessage = model.getAttribute("message");

        assertThat(view).isEqualTo("errors/404");
        assertThat(actualExceptionMessage).isEqualTo(expectedException.getMessage());
    }

    /**
     * Тест на метод delete()
     * Успешное удаление кандидата. Переход на страницу candidates.
     */
    @Test
    public void whenDeleteCandidateByIdThenTrue() {
        var id = 1;
        var isDeleted = true;
        when(candidateService.deleteById(id)).thenReturn(isDeleted);

        var model = new ConcurrentModel();
        var view = candidateController.delete(model, id);
        var actualDeleted = candidateService.deleteById(id);

        assertThat(view).isEqualTo("redirect:/candidates");
        assertThat(actualDeleted).isEqualTo(isDeleted);
    }

    /**
     * Тест на метод delete()
     * Удаление несуществующего кандидата ведет к странице 404 с сообщением об ошибке.
     */
    @Test
    public void whenDeleteCandidateByIdThenGetErrorPageWithMessage() {
        var expectedException = new RuntimeException(
                "Резюме с указанным идентификатором не найдено");
        var id = 1;
        var isDeleted = false;
        when(candidateService.deleteById(id)).thenReturn(isDeleted);

        var model = new ConcurrentModel();
        var view = candidateController.delete(model, id);
        var actualExceptionMessage = model.getAttribute("message");

        assertThat(view).isEqualTo("errors/404");
        assertThat(actualExceptionMessage).isEqualTo(expectedException.getMessage());
    }


}