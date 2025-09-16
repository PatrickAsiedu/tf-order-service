package app.tradeflows.api.order_service;

import app.tradeflows.api.order_service.exceptions.NotFoundException;
import app.tradeflows.api.order_service.services.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RedisServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RedisService<Object> redisService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Set up the mock behavior for opsForValue()
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testGetItemFound() throws NotFoundException {
        // Arrange
        String key = "testKey";
        Object expectedValue = new Object();
        when(valueOperations.get(eq(key))).thenReturn(expectedValue);

        // Act
        Object result = redisService.getItem(key);

        // Assert
        verify(valueOperations).get(eq(key));
        assertEquals(expectedValue, result);
    }

    @Test
    void testGetItemNotFound() {
        // Arrange
        String key = "testKey";
        when(valueOperations.get(eq(key))).thenReturn(null);

        // Act & Assert
        NotFoundException thrown = assertThrows(NotFoundException.class, () -> redisService.getItem(key));
        assertEquals("Record found for this key: testKey", thrown.getMessage());
    }

    @Test
    void testAddItem() {
        // Arrange
        String key = "testKey";
        Object objectToAdd = new Object();

        // Act
        redisService.addItem(key, objectToAdd);

        // Assert
        verify(valueOperations).set(eq(key), eq(objectToAdd));
    }

    @Test
    void testDeleteItem() {
        // Arrange
        String key = "testKey";
        when(redisTemplate.delete(eq(key))).thenReturn(true);

        // Act
        Boolean result = redisService.deleteItem(key);

        // Assert
        verify(redisTemplate).delete(eq(key));
        assertTrue(result);
    }
}
