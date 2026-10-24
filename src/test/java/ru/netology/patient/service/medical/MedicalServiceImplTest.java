package ru.netology.patient.service.medical;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.netology.patient.entity.BloodPressure;
import ru.netology.patient.entity.HealthInfo;
import ru.netology.patient.entity.PatientInfo;
import ru.netology.patient.repository.PatientInfoRepository;
import ru.netology.patient.service.alert.SendAlertService;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты MedicalServiceImpl (Mockito)")
class MedicalServiceImplTest {

    @Mock
    private PatientInfoRepository patientInfoRepository;

    @Mock
    private SendAlertService alertService;

    @InjectMocks
    private MedicalServiceImpl medicalService;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    @Test
    @DisplayName("checkBloodPressure: давление не совпадает с нормой → отправляется сообщение")
    void testCheckBloodPressure_abnormal_sendsAlert() {

        String patientId = "id-1";
        BloodPressure normalPressure = new BloodPressure(120, 80);
        BloodPressure currentPressure = new BloodPressure(150, 95);

        PatientInfo patient = createPatient(patientId, normalPressure, new BigDecimal("36.6"));
        when(patientInfoRepository.getById(patientId)).thenReturn(patient);

        medicalService.checkBloodPressure(patientId, currentPressure);

        verify(alertService, times(1)).send(messageCaptor.capture());
        String sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertTrue(sentMessage.contains(patientId));
        assertTrue(sentMessage.contains("need help"));
    }

    @Test
    @DisplayName("checkTemperature: температура ниже нормы более чем на 1.5 → отправляется сообщение")
    void testCheckTemperature_abnormal_sendsAlert() {

        String patientId = "id-2";
        BigDecimal normalTemperature = new BigDecimal("36.6");

        BigDecimal currentTemperature = new BigDecimal("34.0");

        PatientInfo patient = createPatient(patientId, new BloodPressure(120, 80), normalTemperature);
        when(patientInfoRepository.getById(patientId)).thenReturn(patient);

        medicalService.checkTemperature(patientId, currentTemperature);

        verify(alertService, times(1)).send(messageCaptor.capture());
        String sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertTrue(sentMessage.contains(patientId));
        assertTrue(sentMessage.contains("need help"));
    }

    @Test
    @DisplayName("checkBloodPressure: давление совпадает с нормой → сообщение НЕ отправляется")
    void testCheckBloodPressure_normal_doesNotSendAlert() {

        String patientId = "id-3";
        BloodPressure normalPressure = new BloodPressure(120, 80);

        PatientInfo patient = createPatient(patientId, normalPressure, new BigDecimal("36.6"));
        when(patientInfoRepository.getById(patientId)).thenReturn(patient);

        medicalService.checkBloodPressure(patientId, normalPressure);

        verify(alertService, never()).send(anyString());
    }

    @Test
    @DisplayName("checkTemperature: температура в норме → сообщение НЕ отправляется")
    void testCheckTemperature_normal_doesNotSendAlert() {

        String patientId = "id-4";
        BigDecimal normalTemperature = new BigDecimal("36.6");
        BigDecimal currentTemperature = new BigDecimal("36.6");   // норма

        PatientInfo patient = createPatient(patientId, new BloodPressure(120, 80), normalTemperature);
        when(patientInfoRepository.getById(patientId)).thenReturn(patient);

        medicalService.checkTemperature(patientId, currentTemperature);

        verify(alertService, never()).send(anyString());
    }

    @Test
    @DisplayName("Если пациент не найден → RuntimeException")
    void testCheckBloodPressure_patientNotFound_throwsException() {

        String unknownId = "unknown";
        when(patientInfoRepository.getById(unknownId)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> medicalService.checkBloodPressure(unknownId, new BloodPressure(120, 80)));

        assertEquals("Patient not found", ex.getMessage());
        verifyNoInteractions(alertService);
    }

    private PatientInfo createPatient(String id, BloodPressure pressure, BigDecimal normalTemperature) {
        HealthInfo healthInfo = new HealthInfo(normalTemperature, pressure);
        return new PatientInfo(id, "Иван", "Петров",
                LocalDate.of(1980, 11, 26), healthInfo);
    }
}