package com.ethereal.order.service;

import com.ethereal.order.config.EmailConfiguration;
import com.ethereal.order.entity.Order;
import com.ethereal.order.entity.OrderItems;
import com.ethereal.order.entity.SimpleProductDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailConfiguration emailConfiguration;
    private final RestTemplate restTemplate;

    @Value("${front.url}")
    private String FRONTEND_URL;

    public SimpleProductDTO getProductDetails(String productUuid) {
        String url = "http://localhost:8888/api/v1/product/" + productUuid;
        try {
            ResponseEntity<SimpleProductDTO> response = restTemplate.getForEntity(url, SimpleProductDTO.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch product details for UUID: {}", productUuid, e);
            throw new RuntimeException("Unable to fetch product details");
        }
    }

    private String generateOrderDetailsHtml(Order order, List<OrderItems> items) {
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<html><body style=\"background-color:#f8f5f5; font-family:Georgia, serif; text-align:center;\">");
        htmlBuilder.append("<div style=\"max-width:600px; margin:0 auto; background:#ffffff; padding:30px; box-shadow:0 0 20px rgba(140, 100, 100, 0.1); text-align:center;\">");

        htmlBuilder.append("<h1 style=\"color:#8c6464;\">Szczegóły Twojego zamówienia</h1>");
        htmlBuilder.append("<p>Numer zamówienia: <strong>").append(order.getOrders()).append("</strong></p>");
        htmlBuilder.append("<p>Status zamówienia: <strong>").append(order.getStatus()).append("</strong></p>");
        htmlBuilder.append("<p>Kwota zamówienia: <strong>").append(calculateTotalPrice(items)).append(" zł</strong></p>");

        htmlBuilder.append("<h2 style=\"color:#8c6464;\">Dane klienta</h2>");
        htmlBuilder.append("<p>Imię: ").append(order.getFirstName()).append("</p>");
        htmlBuilder.append("<p>Nazwisko: ").append(order.getLastName()).append("</p>");
        if (order.isCompany()) {
            htmlBuilder.append("<p>Nazwa firmy: ").append(order.getCompanyName()).append("</p>");
            htmlBuilder.append("<p>NIP: ").append(order.getNip()).append("</p>");
        } else {
            htmlBuilder.append("<p>Osoba prywatna</p>");
        }

        htmlBuilder.append("<h2 style=\"color:#8c6464;\">Adres dostawy</h2>");
        htmlBuilder.append("<p>").append(order.getStreet()).append(" ").append(order.getNumber()).append("</p>");
        htmlBuilder.append("<p>").append(order.getPostCode()).append(" ").append(order.getCity()).append("</p>");

        htmlBuilder.append("<h2 style=\"color:#8c6464;\">Produkty</h2>");
        htmlBuilder.append("<table style=\"margin: 0 auto; border-collapse: collapse; width: 100%;\">");
        htmlBuilder.append("<thead><tr><th style='padding: 10px;'>Zdjęcie</th><th style='padding: 10px;'>Nazwa</th><th style='padding: 10px;'>Ilość</th><th style='padding: 10px;'>Cena</th></tr></thead>");
        htmlBuilder.append("<tbody>");

        for (OrderItems item : items) {
            SimpleProductDTO productDetails = getProductDetails(item.getProduct());
            String imageUrl = "http://localhost:8088/api/v1/image?uuid=" + productDetails.getImageUrl();

            htmlBuilder.append("<tr>");
            htmlBuilder.append("<td style='padding: 10px;'><img src='").append(imageUrl).append("' alt='Zdjęcie produktu' width='60' height='60' style='border-radius: 5px;'/></td>");
            htmlBuilder.append("<td style='padding: 10px;'>").append(productDetails.getName()).append("</td>");
            htmlBuilder.append("<td style='padding: 10px;'>").append(item.getQuantity()).append("</td>");
            htmlBuilder.append("<td style='padding: 10px;'>").append(item.getPriceSummary()).append(" zł</td>");
            htmlBuilder.append("</tr>");
        }

        htmlBuilder.append("</tbody></table>");

        htmlBuilder.append("<p style='margin-top: 30px; color:#aaa; font-size: 12px;'>Dziękujemy za zakupy w Ethereal Charm!</p>");

        htmlBuilder.append("</div>");
        htmlBuilder.append("</body></html>");
        return htmlBuilder.toString();
    }

    private double calculateTotalPrice(List<OrderItems> items) {
        return items.stream().mapToDouble(OrderItems::getPriceSummary).sum();
    }

    public void sendActivation(String mail, Order order, List<OrderItems> items) {
        log.info("--START sendOrder");
        try {
            String html = generateOrderDetailsHtml(order, items);
            emailConfiguration.sendMail(mail, html, "Szczegóły zamówienia", true);
        } catch (Exception e) {
            log.error("Nie udało się wysłać e-maila z zamówieniem", e);
            throw new RuntimeException(e);
        }
        log.info("--STOP sendOrder");
    }
}
