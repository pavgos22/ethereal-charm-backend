package com.ethereal.productservice.service;

import com.ethereal.productservice.entity.ProductEntity;
import com.ethereal.productservice.entity.SimpleProductDTO;
import com.ethereal.productservice.repository.CategoryRepository;
import com.ethereal.productservice.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {
    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${file-service.url}")
    private String FILE_SERVICE;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public long countActiveProducts(String name, String category, Float price_min, Float price_max) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<ProductEntity> root = query.from(ProductEntity.class);
        List<Predicate> predicates = prepareQuery(name, category, price_min, price_max, criteriaBuilder, root);
        query.select(criteriaBuilder.count(root)).where(predicates.toArray(new Predicate[0]));
        return entityManager.createQuery(query).getSingleResult();
    }

    public List<ProductEntity> getProduct(String name,
                                          String category,
                                          Float price_min,
                                          Float price_max,
                                          String data,
                                          int page,
                                          int limit,
                                          String sort,
                                          String order) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ProductEntity> query = criteriaBuilder.createQuery(ProductEntity.class);
        Root<ProductEntity> root = query.from(ProductEntity.class);

        if (data != null && !data.isEmpty() && name != null && !name.trim().isEmpty()) {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate date = LocalDate.parse(data, inputFormatter);
            return productRepository.findByNameAndCreateAt(name, date);
        }

        if (page <= 0) page = 1;

        List<Predicate> predicates = prepareQuery(name, category, price_min, price_max, criteriaBuilder, root);

        if (sort == null || sort.isEmpty()) {
            sort = "createAt";
        }
        if (order == null || order.isEmpty()) {
            order = "desc";
        }

        Expression<?> sortColumn;
        if ("price".equals(sort)) {
            sortColumn = criteriaBuilder.selectCase()
                    .when(criteriaBuilder.isTrue(root.get("discount")), root.get("discountedPrice"))
                    .otherwise(root.get("price"));
        } else {
            sortColumn = switch (sort) {
                case "name" -> root.get("name");
                case "category" -> root.get("category").get("name");
                case "data" -> root.get("createAt");
                default -> root.get("priority");
            };
        }

        System.out.println(sortColumn);

        Order orderQuery = "desc".equals(order)
                ? criteriaBuilder.desc(sortColumn)
                : criteriaBuilder.asc(sortColumn);
        query.orderBy(orderQuery);

        query.where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(query)
                .setFirstResult((page - 1) * limit)
                .setMaxResults(limit)
                .getResultList();
    }


    public SimpleProductDTO getSimpleProductByUuid(String uuid) {
        ProductEntity productEntity = productRepository.findByUid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Product with UID " + uuid + " not found"));

        return new SimpleProductDTO(
                productEntity.getUid(),
                productEntity.getName(),
                productEntity.getMainDesc(),
                productEntity.getPrice(),
                productEntity.getImageUrls().length > 0 ? productEntity.getImageUrls()[0] : null,
                productEntity.getCreateAt(),
                productEntity.isActivate(),
                productEntity.getDiscountedPrice(),
                productEntity.getPriority()
        );
    }

    private List<Predicate> prepareQuery(String name,
                                         String category,
                                         Float price_min,
                                         Float price_max,
                                         CriteriaBuilder criteriaBuilder,
                                         Root<ProductEntity> root) {
        List<Predicate> predicates = new ArrayList<>();
        if (name != null && !name.trim().isEmpty())
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
        if (category != null && !category.isEmpty()) {
            categoryRepository.findByShortId(category).
                    ifPresent(value -> predicates.add(criteriaBuilder.equal(root.get("category"), value)));
        }
        if (price_min != null)
            predicates.add(criteriaBuilder.greaterThan(root.get("price"), price_min - 0.01));
        if (price_max != null)
            predicates.add(criteriaBuilder.lessThan(root.get("price"), price_max + 0.01));
        predicates.add(criteriaBuilder.isTrue(root.get("activate")));
        return predicates;
    }

    @Transactional
    public void createProduct(ProductEntity product) {
        logger.info("Creating product with details: {}", product);

        if (product != null) {
            product.setCreateAt(LocalDate.now());
            product.setUid(UUID.randomUUID().toString());
            product.setActivate(true);
            productRepository.save(product);

            for (String uuid : product.getImageUrls())
                activateImage(uuid);

            logger.info("Product created successfully with UID: {}", product.getUid());
            return;
        }
        throw new RuntimeException("Product is null, cannot create.");
    }

    private void activateImage(String uuid) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FILE_SERVICE + "?uuid=" + uuid))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void delete(String uuid) throws RuntimeException {
        productRepository.findByUid(uuid).ifPresentOrElse(value -> {
            value.setActivate(false);
            productRepository.save(value);
            for (String image : value.getImageUrls()) {
                deleteImages(image);
            }

        }, () -> {
            throw new RuntimeException();
        });
    }

    private void deleteImages(String uuid) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.delete(FILE_SERVICE + "?uuid=" + uuid);
    }

    public ProductEntity updateProductDiscount(String uuid, boolean discount, float price, float discountedPrice) {
        ProductEntity product = productRepository.findByUid(uuid)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setDiscount(discount);
        product.setPrice(price);
        product.setDiscountedPrice(discountedPrice);

        return productRepository.saveAndFlush(product);
    }

    public ProductEntity updatePriority(String uuid, int priority) {
        ProductEntity product = productRepository.findByUid(uuid)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setPriority(priority);
        return productRepository.saveAndFlush(product);
    }

    public Optional<ProductEntity> getProductByUuid(String uuid) {
        return productRepository.findByUid(uuid);
    }
}