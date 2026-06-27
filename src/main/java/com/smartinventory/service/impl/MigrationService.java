package com.smartinventory.service.impl;

import com.smartinventory.dto.response.MigrationResponse;
import com.smartinventory.entity.*;
import com.smartinventory.enums.ProfileStatus;
import com.smartinventory.enums.Role;
import com.smartinventory.enums.UserStatus;
import com.smartinventory.exception.BadRequestException;
import com.smartinventory.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationService {

    private static final String DEFAULT_PASSWORD = "Smart@1234";
    private static final String ADMIN_EMAIL      = "admin@smartinventory.com";
    private static final int    CATEGORY_COUNT   = 5;
    private static final int    PRODUCT_COUNT      = 500;
    private static final int    RETAILER_COUNT     = 100;
    private static final int    SUPPLIER_COUNT     = 100;
    private static final int    INVENTORY_PER_USER = 50;
    private static final int    BATCH_SIZE         = 500;

    private static final String[] CATEGORY_NAMES = {
        "Electronics",
        "Groceries",
        "Clothing & Apparel",
        "Home & Kitchen",
        "Health & Beauty"
    };

    private static final String[] CATEGORY_DESCRIPTIONS = {
        "Electronic gadgets, accessories, and consumer devices",
        "Daily food items, staples, and packaged groceries",
        "Men's, women's, and children's clothing and fashion",
        "Kitchen appliances, cookware, and home essentials",
        "Personal care, wellness, and beauty products"
    };

    private static final String[][] CATEGORY_BRANDS = {
        {"Samsung", "Sony", "LG", "Boat", "Philips", "Mi", "HP", "Dell", "Lenovo", "Apple"},
        {"Amul", "Nestle", "Britannia", "Parle", "Tata", "MDH", "Haldiram", "Patanjali", "Fortune", "Aashirvaad"},
        {"FabIndia", "Raymond", "Peter England", "Allen Solly", "Levis", "H&M", "Zara", "Wrangler", "US Polo", "Biba"},
        {"Prestige", "Milton", "Borosil", "Cello", "Hawkins", "Pigeon", "Bajaj", "Philips", "Wonderchef", "Tupperware"},
        {"Himalaya", "Dove", "L'Oreal", "Nivea", "Patanjali", "Colgate", "Pepsodent", "Garnier", "Lakme", "Biotique"}
    };

    private static final String[] UNITS = {"pcs", "kg", "litre", "box", "pack", "pair", "set"};

    private static final String[] CITIES = {
        "Mumbai", "Delhi", "Bangalore", "Hyderabad", "Chennai",
        "Pune", "Kolkata", "Ahmedabad", "Jaipur", "Lucknow"
    };

    private static final String[] STATES = {
        "Maharashtra", "Delhi", "Karnataka", "Telangana", "Tamil Nadu",
        "Maharashtra", "West Bengal", "Gujarat", "Rajasthan", "Uttar Pradesh"
    };

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final RetailerInventoryRepository retailerInventoryRepository;
    private final SupplierInventoryRepository supplierInventoryRepository;
    private final NotificationRepository notificationRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final RatingRepository ratingRepository;
    private final SupplierInventoryImageRepository supplierInventoryImageRepository;
    private final OtpRepository otpRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final RetailerProductRecommendationRepository retailerProductRecommendationRepository;
    private final RetailerSaleItemRepository retailerSaleItemRepository;
    private final RetailerSaleRepository retailerSaleRepository;
    private final RetailerSalesAnalyticsRepository retailerSalesAnalyticsRepository;

    @Transactional
    public MigrationResponse runMigration(boolean force) {
        if (!force && productRepository.count() >= PRODUCT_COUNT) {
            throw new BadRequestException(
                "Migration already completed. Pass force=true to reset and reseed.");
        }

        if (force) {
            log.info("Force migration: clearing existing data");
            clearAllData();
        }

        List<Category> categories = seedCategories();
        List<Product> products = seedProducts(categories);
        User admin = seedAdmin();
        List<User> retailers = seedUsers(Role.RETAILER, RETAILER_COUNT);
        List<User> suppliers = seedUsers(Role.SUPPLIER, SUPPLIER_COUNT);

        seedProfile(admin, "System", "Admin", "Smart Inventory HQ", "Platform", 0);
        for (int i = 0; i < retailers.size(); i++) {
            seedProfile(retailers.get(i), "Retailer", "User" + (i + 1),
                "Retailer Store " + (i + 1), "Retail", i);
        }
        for (int i = 0; i < suppliers.size(); i++) {
            seedProfile(suppliers.get(i), "Supplier", "User" + (i + 1),
                "Supplier Traders " + (i + 1), "Wholesale", i);
        }

        int retailerInventoryCount = seedRetailerInventories(retailers, products);
        int supplierInventoryCount = seedSupplierInventories(suppliers, products);
        int ratingsCount = seedRatings(retailers, suppliers);

        log.info("Migration completed successfully");

        return MigrationResponse.builder()
            .categoriesCreated(categories.size())
            .productsCreated(products.size())
            .retailersCreated(retailers.size())
            .suppliersCreated(suppliers.size())
            .adminsCreated(1)
            .retailerInventoryRecords(retailerInventoryCount)
            .supplierInventoryRecords(supplierInventoryCount)
            .ratingsCreated(ratingsCount)
            .defaultPassword(DEFAULT_PASSWORD)
            .adminEmail(ADMIN_EMAIL)
            .build();
    }

    private void clearAllData() {
        retailerSalesAnalyticsRepository.deleteAllInBatch();
        retailerProductRecommendationRepository.deleteAllInBatch();
        notificationRepository.deleteAllInBatch();
        retailerSaleItemRepository.deleteAllInBatch();
        retailerSaleRepository.deleteAllInBatch();
        orderItemRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        ratingRepository.deleteAllInBatch();
        supplierInventoryImageRepository.deleteAllInBatch();
        supplierInventoryRepository.deleteAllInBatch();
        retailerInventoryRepository.deleteAllInBatch();
        otpRepository.deleteAllInBatch();
        subscriptionRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        profileRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        entityManager.flush();
        entityManager.clear();
    }

    private List<Category> seedCategories() {
        List<Category> categories = new ArrayList<>();
        for (int i = 0; i < CATEGORY_COUNT; i++) {
            Category category = Category.builder()
                .name(CATEGORY_NAMES[i])
                .description(CATEGORY_DESCRIPTIONS[i])
                .build();
            categories.add(category);
        }
        return categoryRepository.saveAll(categories);
    }

    private List<Product> seedProducts(List<Category> categories) {
        List<Product> products = new ArrayList<>(PRODUCT_COUNT);
        int productsPerCategory = PRODUCT_COUNT / CATEGORY_COUNT;

        for (int catIndex = 0; catIndex < categories.size(); catIndex++) {
            Category category = categories.get(catIndex);
            String[] brands = CATEGORY_BRANDS[catIndex];
            String categoryCode = category.getName()
                .replaceAll("[^A-Za-z]", "")
                .substring(0, Math.min(3, category.getName().replaceAll("[^A-Za-z]", "").length()))
                .toUpperCase();

            for (int i = 1; i <= productsPerCategory; i++) {
                int globalIndex = catIndex * productsPerCategory + i;
                String brand = brands[(i - 1) % brands.length];
                String unit = UNITS[(i - 1) % UNITS.length];
                String name = String.format("%s %s %03d", brand, category.getName(), i);

                Product product = Product.builder()
                    .category(category)
                    .name(name)
                    .description(String.format(
                        "Premium quality %s from %s. Suitable for everyday use. Product #%d in %s category.",
                        name, brand, globalIndex, category.getName()))
                    .sku(String.format("SKU-%s-%04d", categoryCode, globalIndex))
                    .unit(unit)
                    .brand(brand)
                    .build();
                products.add(product);
            }
        }

        return saveInBatches(products, productRepository::saveAll);
    }

    private User seedAdmin() {
        return userRepository.findByEmail(ADMIN_EMAIL)
            .map(existing -> {
                existing.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
                existing.setRole(Role.ADMIN);
                existing.setStatus(UserStatus.ACTIVE);
                existing.setProfileStatus(ProfileStatus.COMPLETED);
                existing.setEmailVerifiedAt(LocalDateTime.now());
                existing.setAdminVerifiedAt(LocalDateTime.now());
                return userRepository.save(existing);
            })
            .orElseGet(() -> userRepository.save(buildUser(ADMIN_EMAIL, Role.ADMIN)));
    }

    private List<User> seedUsers(Role role, int count) {
        List<User> users = new ArrayList<>(count);
        String prefix = role == Role.RETAILER ? "retailer" : "supplier";

        for (int i = 1; i <= count; i++) {
            String email = String.format("%s%03d@smartinventory.com", prefix, i);
            users.add(buildUser(email, role));
        }

        return saveInBatches(users, userRepository::saveAll);
    }

    private User buildUser(String email, Role role) {
        LocalDateTime now = LocalDateTime.now();
        return User.builder()
            .email(email)
            .password(passwordEncoder.encode(DEFAULT_PASSWORD))
            .role(role)
            .status(UserStatus.ACTIVE)
            .profileStatus(ProfileStatus.COMPLETED)
            .emailVerifiedAt(now)
            .adminVerifiedAt(now)
            .lastLoginAt(now)
            .build();
    }

    private void seedProfile(User user, String firstName, String lastName,
                             String businessName, String businessType, int locationIndex) {
        if (profileRepository.findByUserId(user.getId()).isPresent()) {
            return;
        }
        int idx = locationIndex % CITIES.length;
        Profile profile = Profile.builder()
            .user(user)
            .firstName(firstName)
            .lastName(lastName)
            .businessName(businessName)
            .businessType(businessType)
            .mobNo(String.format("9%09d", ThreadLocalRandom.current().nextInt(0, 1_000_000_000)))
            .address(String.format("%d, Main Street, %s", 100 + locationIndex, CITIES[idx]))
            .gst(String.format("%02dABCDE%04dF1Z5", idx + 1, locationIndex + 1))
            .city(CITIES[idx])
            .state(STATES[idx])
            .pincode(String.format("%06d", 400000 + locationIndex))
            .profileImageUrl(user.getRole() == Role.ADMIN ? "smart_inventory_logo.png" : null)
            .build();
        profileRepository.save(profile);
    }

    private int seedRetailerInventories(List<User> retailers, List<Product> products) {
        List<RetailerInventory> inventories = new ArrayList<>(RETAILER_COUNT * INVENTORY_PER_USER);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int r = 0; r < retailers.size(); r++) {
            User retailer = retailers.get(r);
            for (int p = 0; p < INVENTORY_PER_USER; p++) {
                Product product = products.get((r * INVENTORY_PER_USER + p) % products.size());
                int threshold = 10 + (p % 15);
                int quantity = random.nextInt(5, 120);
                if (p % 5 == 0) {
                    quantity = random.nextInt(1, threshold);
                }

                BigDecimal price = randomPrice(50, 5000);

                inventories.add(RetailerInventory.builder()
                    .user(retailer)
                    .product(product)
                    .quantity(quantity)
                    .thresholdValue(threshold)
                    .price(price)
                    .build());
            }
        }

        saveInBatches(inventories, retailerInventoryRepository::saveAll);
        return inventories.size();
    }

    private int seedSupplierInventories(List<User> suppliers, List<Product> products) {
        List<SupplierInventory> inventories = new ArrayList<>(SUPPLIER_COUNT * INVENTORY_PER_USER);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int s = 0; s < suppliers.size(); s++) {
            User supplier = suppliers.get(s);
            for (int p = 0; p < INVENTORY_PER_USER; p++) {
                Product product = products.get((s * 7 + p * 3) % products.size());
                int moq = 1 + (p % 20);
                int stockQuantity = 100 + random.nextInt(4900);
                int leadTime = 1 + random.nextInt(14);
                LocalDate availability = LocalDate.now().plusDays(random.nextInt(1, 31));

                inventories.add(SupplierInventory.builder()
                    .user(supplier)
                    .product(product)
                    .price(randomPrice(30, 4500))
                    .moq(moq)
                    .stockQuantity(stockQuantity)
                    .availability(availability)
                    .leadTime(leadTime)
                    .isActive(true)
                    .build());
            }
        }

        saveInBatches(inventories, supplierInventoryRepository::saveAll);
        return inventories.size();
    }

    private BigDecimal randomPrice(int min, int max) {
        double value = ThreadLocalRandom.current().nextDouble(min, max);
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private <T> List<T> saveInBatches(List<T> items, java.util.function.Function<List<T>, List<T>> saver) {
        List<T> saved = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, items.size());
            saved.addAll(saver.apply(items.subList(i, end)));
            entityManager.flush();
            entityManager.clear();
        }
        return saved;
    }

    private int seedRatings(List<User> retailers, List<User> suppliers) {
        List<Rating> ratings = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        String[] reviews = {
            "Excellent service, very fast delivery!",
            "Good product quality, but slightly delayed delivery.",
            "Best wholesale prices in the region.",
            "Highly recommended supplier!",
            "Satisfactory products and decent MOQ.",
            "Great communication and reliable packaging.",
            "MOQ is a bit high, but product quality is top-notch.",
            "Prompt response and very polite staff.",
            "Good value for money.",
            "Items received in perfect condition. Thank you!"
        };

        for (User supplier : suppliers) {
            // Seed 3 to 6 random ratings per supplier
            int ratingCount = random.nextInt(3, 7);
            List<User> pickedRetailers = new ArrayList<>();
            
            for (int i = 0; i < ratingCount; i++) {
                User retailer = retailers.get(random.nextInt(retailers.size()));
                if (pickedRetailers.contains(retailer)) {
                    continue;
                }
                pickedRetailers.add(retailer);
                
                int ratingVal = random.nextInt(3, 6); // ratings between 3 and 5 stars
                if (random.nextDouble() < 0.1) {
                    ratingVal = 2; // 10% chance of a 2-star rating
                }
                
                String review = reviews[random.nextInt(reviews.length)];
                
                Rating rating = Rating.builder()
                        .supplier(supplier)
                        .retailer(retailer)
                        .rating(ratingVal)
                        .review(review)
                        .build();
                ratings.add(rating);
            }
        }
        
        saveInBatches(ratings, ratingRepository::saveAll);
        return ratings.size();
    }
}
