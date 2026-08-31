SET NAMES utf8mb4;

CREATE TABLE `account_staffs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `login_id` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nickname` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email_verified_at` timestamp(6) NULL DEFAULT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `department` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `job_title` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `last_login_at` timestamp(6) NULL DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `deleted_at` timestamp(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `account_staffs_nickname_unique` (`nickname`),
  UNIQUE KEY `account_staffs_email_unique` (`email`),
  UNIQUE KEY `account_staffs_login_id_unique` (`login_id`),
  KEY `account_staffs_status_index` (`status`),
  KEY `account_staffs_login_status_idx` (`last_login_at`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='내부 운영자 계정 테이블';

CREATE TABLE `staff_roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `staff_roles_name_unique` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='내부 운영자 역할 테이블';

CREATE TABLE `staff_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `staff_permissions_code_unique` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='내부 운영자 권한 테이블';

CREATE TABLE `staff_role_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `staff_role_id` bigint NOT NULL,
  `staff_permission_id` bigint NOT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `staff_role_permissions_role_permission_unique` (`staff_role_id`,`staff_permission_id`),
  KEY `staff_role_permissions_permission_id_index` (`staff_permission_id`),
  CONSTRAINT `fk_staff_role_permissions_permission` FOREIGN KEY (`staff_permission_id`) REFERENCES `staff_permissions` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_staff_role_permissions_role` FOREIGN KEY (`staff_role_id`) REFERENCES `staff_roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='내부 운영자 역할 권한 연결 테이블';

CREATE TABLE `account_staff_roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_staff_id` bigint NOT NULL,
  `staff_role_id` bigint NOT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `account_staff_roles_staff_role_unique` (`account_staff_id`,`staff_role_id`),
  KEY `account_staff_roles_role_id_index` (`staff_role_id`),
  CONSTRAINT `fk_account_staff_roles_role` FOREIGN KEY (`staff_role_id`) REFERENCES `staff_roles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_account_staff_roles_staff` FOREIGN KEY (`account_staff_id`) REFERENCES `account_staffs` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='내부 운영자 계정 역할 연결 테이블';

CREATE TABLE `account_users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nickname` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `signup_channel` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'EMAIL',
  `email_verified_at` timestamp(6) NULL DEFAULT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `warning_count` int NOT NULL DEFAULT '0',
  `blocked_at` timestamp(6) NULL DEFAULT NULL,
  `withdrawal_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `comment_notification_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `note_notification_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `marketing_sms_agreed` tinyint(1) NOT NULL DEFAULT '0',
  `marketing_email_agreed` tinyint(1) NOT NULL DEFAULT '0',
  `marketing_push_agreed` tinyint(1) NOT NULL DEFAULT '0',
  `marketing_night_push_agreed` tinyint(1) NOT NULL DEFAULT '0',
  `last_login_at` timestamp(6) NULL DEFAULT NULL,
  `last_accessed_at` timestamp(6) NULL DEFAULT NULL,
  `last_access_ip` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `deleted_at` timestamp(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `account_users_nickname_unique` (`nickname`),
  UNIQUE KEY `account_users_email_unique` (`email`),
  KEY `account_users_status_index` (`status`),
  KEY `account_users_login_status_idx` (`last_login_at`,`status`),
  KEY `account_users_last_accessed_idx` (`last_accessed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일반 사용자 계정 테이블';

CREATE TABLE `categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `domain` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `depth` tinyint NOT NULL,
  `group_code` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `full_path` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `is_menu_visible` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `categories_domain_code_unique` (`domain`,`code`),
  KEY `categories_parent_id_index` (`parent_id`),
  KEY `categories_domain_status_sort_index` (`domain`,`status`,`sort_order`),
  KEY `categories_domain_depth_sort_id_idx` (`domain`,`depth`,`sort_order`,`id`),
  KEY `categories_domain_group_status_sort_id_idx` (`domain`,`group_code`,`status`,`sort_order`,`id`),
  KEY `categories_domain_parent_sort_id_idx` (`domain`,`parent_id`,`sort_order`,`id`),
  KEY `categories_domain_parent_status_sort_id_idx` (`domain`,`parent_id`,`status`,`sort_order`,`id`),
  KEY `categories_domain_visible_sort_id_idx` (`domain`,`is_menu_visible`,`sort_order`,`id`),
  CONSTRAINT `fk_categories_parent` FOREIGN KEY (`parent_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='카테고리 테이블';

CREATE TABLE `category_usages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `usage_code` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_id` bigint NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `category_usages_usage_category_unique` (`usage_code`,`category_id`),
  KEY `category_usages_usage_status_sort_id_idx` (`usage_code`,`status`,`sort_order`,`id`),
  KEY `category_usages_usage_status_category_idx` (`usage_code`,`status`,`category_id`),
  KEY `category_usages_category_id_index` (`category_id`),
  CONSTRAINT `fk_category_usages_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='카테고리 사용처별 노출 매핑';

CREATE TABLE `category_assignments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `categorizable_type` varchar(191) COLLATE utf8mb4_unicode_ci NOT NULL,
  `categorizable_id` bigint NOT NULL,
  `category_id` bigint NOT NULL,
  `is_primary` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `category_assignments_target_category_unique` (`categorizable_type`,`categorizable_id`,`category_id`),
  KEY `category_assignments_category_id_index` (`category_id`),
  KEY `category_assignments_target_index` (`categorizable_type`,`categorizable_id`),
  CONSTRAINT `fk_category_assignments_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='카테고리 연결 테이블';

CREATE TABLE `partner_features` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int unsigned NOT NULL DEFAULT '0',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `partner_features_code_unique` (`code`),
  KEY `partner_features_status_sort_order_index` (`status`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='파트너 정보 마스터 테이블';

CREATE TABLE `partners` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `english_name` varchar(90) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `road_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `detail_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `subway_stations` json DEFAULT NULL,
  `region_sort_key` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `latitude` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `longitude` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operating_hours_notice` text COLLATE utf8mb4_unicode_ci,
  `operation_hours` json DEFAULT NULL,
  `holiday_policy` json DEFAULT NULL,
  `direction` text COLLATE utf8mb4_unicode_ci,
  `view_count` bigint unsigned NOT NULL DEFAULT '0',
  `evaluation_count` int unsigned NOT NULL DEFAULT '0',
  `evaluation_average_rating` decimal(2,1) NOT NULL DEFAULT '0.0',
  `allow_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'REVIEW_REQUESTED',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `registration_source` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'STAFF_CREATED',
  `created_by_staff_id` bigint DEFAULT NULL,
  `assigned_staff_id` bigint DEFAULT NULL,
  `reviewer_staff_id` bigint DEFAULT NULL,
  `review_started_at` timestamp(6) NULL DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `deleted_at` timestamp(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `partners_allow_status_index` (`allow_status`),
  KEY `partners_status_index` (`status`),
  KEY `partners_view_count_index` (`view_count`),
  KEY `partners_evaluation_count_index` (`evaluation_count`),
  KEY `partners_evaluation_average_rating_index` (`evaluation_average_rating`),
  KEY `partners_deleted_id_idx` (`deleted_at`,`id`),
  KEY `partners_deleted_name_id_idx` (`deleted_at`,`name`,`id`),
  KEY `partners_deleted_created_id_idx` (`deleted_at`,`created_at`,`id`),
  KEY `partners_deleted_updated_id_idx` (`deleted_at`,`updated_at`,`id`),
  KEY `partners_deleted_status_id_idx` (`deleted_at`,`status`,`id`),
  KEY `partners_deleted_allow_status_id_idx` (`deleted_at`,`allow_status`,`id`),
  KEY `partners_deleted_allow_created_id_idx` (`deleted_at`,`allow_status`,`created_at`,`id`),
  KEY `partners_deleted_status_created_id_idx` (`deleted_at`,`status`,`created_at`,`id`),
  KEY `partners_assigned_staff_id_index` (`assigned_staff_id`),
  KEY `partners_reviewer_staff_id_index` (`reviewer_staff_id`),
  KEY `partners_registration_source_index` (`registration_source`),
  KEY `partners_created_by_staff_index` (`created_by_staff_id`),
  KEY `partners_region_sort_key_index` (`region_sort_key`),
  CONSTRAINT `fk_partners_assigned_staff` FOREIGN KEY (`assigned_staff_id`) REFERENCES `account_staffs` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_partners_reviewer_staff` FOREIGN KEY (`reviewer_staff_id`) REFERENCES `account_staffs` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_partners_created_by_staff` FOREIGN KEY (`created_by_staff_id`) REFERENCES `account_staffs` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='파트너 테이블';

CREATE TABLE `account_partners` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `partner_id` bigint NOT NULL,
  `login_id` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email_verified_at` timestamp(6) NULL DEFAULT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `last_login_at` timestamp(6) NULL DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `deleted_at` timestamp(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `account_partners_partner_id_unique` (`partner_id`),
  UNIQUE KEY `account_partners_email_unique` (`email`),
  UNIQUE KEY `account_partners_login_id_unique` (`login_id`),
  KEY `account_partners_status_index` (`status`),
  KEY `account_partners_login_status_partner_idx` (`last_login_at`,`status`,`partner_id`),
  KEY `account_partners_status_partner_idx` (`status`,`partner_id`),
  CONSTRAINT `fk_account_partners_partner` FOREIGN KEY (`partner_id`) REFERENCES `partners` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='파트너 관리자 계정 테이블';

CREATE TABLE `partner_contacts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `partner_id` bigint NOT NULL,
  `contact_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `value` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` tinyint NOT NULL DEFAULT '0',
  `is_primary` tinyint(1) NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `verified_at` timestamp(6) NULL DEFAULT NULL,
  `memo` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `deleted_at` timestamp(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `partner_contacts_type_order_index` (`partner_id`,`contact_type`,`sort_order`),
  CONSTRAINT `fk_partner_contacts_partner` FOREIGN KEY (`partner_id`) REFERENCES `partners` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='파트너 연락처 테이블';

CREATE TABLE `partner_business_registrations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `partner_id` bigint NOT NULL,
  `business_number` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `company_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ceo_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `business_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `business_item` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `business_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `business_address_detail` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `settlement_bank_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `settlement_account_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `settlement_account_holder` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `partner_business_registrations_business_number_unique` (`business_number`),
  UNIQUE KEY `partner_business_registrations_partner_id_unique` (`partner_id`),
  KEY `partner_business_registrations_status_index` (`status`),
  CONSTRAINT `fk_partner_business_registrations_partner` FOREIGN KEY (`partner_id`) REFERENCES `partners` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='파트너 사업자 등록 테이블';

CREATE TABLE `partner_feature_assignments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `partner_id` bigint NOT NULL,
  `partner_feature_id` bigint NOT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `partner_feature_assignments_partner_feature_unique` (`partner_id`,`partner_feature_id`),
  KEY `partner_feature_assignments_partner_feature_id_index` (`partner_feature_id`),
  CONSTRAINT `fk_partner_feature_assignments_feature` FOREIGN KEY (`partner_feature_id`) REFERENCES `partner_features` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_partner_feature_assignments_partner` FOREIGN KEY (`partner_id`) REFERENCES `partners` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='파트너 특징 연결 테이블';

CREATE TABLE `partner_links` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `partner_id` bigint NOT NULL,
  `link_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `url` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` tinyint NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `partner_links_partner_type_unique` (`partner_id`,`link_type`),
  CONSTRAINT `fk_partner_links_partner` FOREIGN KEY (`partner_id`) REFERENCES `partners` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `partner_specialists` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `partner_id` bigint NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '스페셜리스트명',
  `gender` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '성별(남, 여)',
  `position` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '직책',
  `career_started_at` date DEFAULT NULL COMMENT '총 경력 시작일',
  `specialist_field` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '전문가 분야',
  `introduction` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `schedule_mode` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'INHERIT_PARTNER_HOURS',
  `operation_hours` json DEFAULT NULL,
  `holiday_policy` json NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'HIDDEN',
  `allow_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'REVIEW_REQUESTED',
  `reviewer_staff_id` bigint DEFAULT NULL,
  `review_started_at` timestamp(6) NULL DEFAULT NULL,
  `view_count` bigint NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `deleted_at` timestamp(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `partner_specialists_partner_id_sort_order_index` (`partner_id`,`sort_order`),
  KEY `partner_specialists_status_index` (`status`),
  KEY `partner_specialists_allow_status_index` (`allow_status`),
  KEY `partner_specialists_reviewer_staff_id_index` (`reviewer_staff_id`),
  KEY `partner_specialists_view_count_index` (`view_count`),
  KEY `partner_specialists_deleted_id_idx` (`deleted_at`,`id`),
  KEY `partner_specialists_deleted_created_id_idx` (`deleted_at`,`created_at`,`id`),
  KEY `partner_specialists_deleted_partner_id_idx` (`deleted_at`,`partner_id`,`id`),
  KEY `partner_specialists_deleted_partner_sort_id_idx` (`deleted_at`,`partner_id`,`sort_order`,`id`),
  KEY `partner_specialists_deleted_allow_status_id_idx` (`deleted_at`,`allow_status`,`id`),
  KEY `partner_specialists_deleted_position_id_idx` (`deleted_at`,`position`,`id`),
  KEY `partner_specialists_deleted_field_id_idx` (`deleted_at`,`specialist_field`,`id`),
  KEY `partner_specialists_deleted_career_id_idx` (`deleted_at`,`career_started_at`,`id`),
  CONSTRAINT `fk_partner_specialists_partner` FOREIGN KEY (`partner_id`) REFERENCES `partners` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_partner_specialists_reviewer_staff` FOREIGN KEY (`reviewer_staff_id`) REFERENCES `account_staffs` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='파트너 소속 스페셜리스트 테이블';

CREATE TABLE `partner_options` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `partner_id` bigint NOT NULL,
  `name` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `regular_price` decimal(12,2) NOT NULL,
  `sale_price` decimal(12,2) DEFAULT NULL,
  `duration_minutes` int DEFAULT NULL,
  `is_visible` tinyint(1) NOT NULL DEFAULT '1',
  `sort_order` int NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `deleted_at` timestamp(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `partner_options_partner_visible_order_index` (`partner_id`,`deleted_at`,`is_visible`,`sort_order`),
  CONSTRAINT `chk_partner_options_prices` CHECK (`regular_price` >= 0 AND (`sale_price` IS NULL OR (`sale_price` >= 0 AND `sale_price` < `regular_price`))),
  CONSTRAINT `fk_partner_options_partner` FOREIGN KEY (`partner_id`) REFERENCES `partners` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `specialist_options` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `specialist_id` bigint NOT NULL,
  `partner_option_id` bigint NOT NULL,
  `regular_price_override` decimal(12,2) DEFAULT NULL,
  `sale_price_override` decimal(12,2) DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `specialist_options_specialist_option_unique` (`specialist_id`,`partner_option_id`),
  KEY `specialist_options_partner_option_index` (`partner_option_id`),
  CONSTRAINT `chk_specialist_options_prices` CHECK ((`regular_price_override` IS NULL AND `sale_price_override` IS NULL) OR (`regular_price_override` >= 0 AND (`sale_price_override` IS NULL OR (`sale_price_override` >= 0 AND `sale_price_override` < `regular_price_override`)))),
  CONSTRAINT `fk_specialist_options_partner_option` FOREIGN KEY (`partner_option_id`) REFERENCES `partner_options` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_specialist_options_specialist` FOREIGN KEY (`specialist_id`) REFERENCES `partner_specialists` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `partner_account_invitations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `partner_id` bigint NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token_hash` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `expires_at` timestamp(6) NOT NULL,
  `sent_at` timestamp(6) NULL DEFAULT NULL,
  `accepted_at` timestamp(6) NULL DEFAULT NULL,
  `canceled_at` timestamp(6) NULL DEFAULT NULL,
  `created_by_staff_id` bigint DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `partner_account_invitations_token_hash_unique` (`token_hash`),
  KEY `partner_account_invitations_partner_created_index` (`partner_id`,`created_at`,`id`),
  KEY `partner_account_invitations_email_status_index` (`email`,`status`,`expires_at`),
  KEY `partner_account_invitations_staff_index` (`created_by_staff_id`),
  CONSTRAINT `fk_partner_account_invitations_partner` FOREIGN KEY (`partner_id`) REFERENCES `partners` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_partner_account_invitations_staff` FOREIGN KEY (`created_by_staff_id`) REFERENCES `account_staffs` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `hashtags` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `normalized_name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `hashtags_normalized_name_unique` (`normalized_name`),
  KEY `hashtags_status_name_index` (`status`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `hashtag_relations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `hashtag_id` bigint NOT NULL,
  `target_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_id` bigint NOT NULL,
  `sort_order` tinyint unsigned NOT NULL DEFAULT '0',
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `hashtag_relations_target_hashtag_unique` (`target_type`,`target_id`,`hashtag_id`),
  UNIQUE KEY `hashtag_relations_target_order_unique` (`target_type`,`target_id`,`sort_order`),
  KEY `hashtag_relations_hashtag_index` (`hashtag_id`),
  CONSTRAINT `fk_hashtag_relations_hashtag` FOREIGN KEY (`hashtag_id`) REFERENCES `hashtags` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `media` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `owner_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `owner_id` bigint NOT NULL,
  `collection` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `disk` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'LOCAL',
  `path` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `mime_type` varchar(127) COLLATE utf8mb4_unicode_ci NOT NULL,
  `size` bigint NOT NULL,
  `width` int DEFAULT NULL,
  `height` int DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `is_primary` tinyint(1) NOT NULL DEFAULT '0',
  `metadata` json DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `deleted_at` timestamp(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `media_path_unique` (`path`),
  KEY `media_owner_collection_order_idx` (`owner_type`,`owner_id`,`collection`,`deleted_at`,`sort_order`,`id`),
  KEY `media_owner_deleted_idx` (`owner_type`,`owner_id`,`deleted_at`,`id`),
  KEY `media_deleted_created_idx` (`deleted_at`,`created_at`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='폴리모픽 미디어 파일 메타데이터 테이블';

CREATE TABLE `operation_histories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `target_type` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_id` bigint NOT NULL,
  `actor_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'STAFF',
  `actor_id` bigint DEFAULT NULL,
  `action` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `memo` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `operation_histories_target_index` (`target_type`,`target_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='운영 처리 이력 테이블';

CREATE TABLE `operation_history_changes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operation_history_id` bigint NOT NULL,
  `field_key` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `before_value` text COLLATE utf8mb4_unicode_ci,
  `after_value` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `operation_history_changes_history_index` (`operation_history_id`),
  CONSTRAINT `fk_operation_history_changes_history` FOREIGN KEY (`operation_history_id`) REFERENCES `operation_histories` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='운영 처리 이력 변경 상세 테이블';

CREATE TABLE `auth_sessions` (
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `actor_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_id` bigint NOT NULL,
  `refresh_token_hash` char(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `previous_refresh_token_hash` char(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `previous_token_valid_until` timestamp(6) NULL DEFAULT NULL,
  `persistent` tinyint(1) NOT NULL DEFAULT '0',
  `expires_at` timestamp(6) NOT NULL,
  `last_used_at` timestamp(6) NOT NULL,
  `revoked_at` timestamp(6) NULL DEFAULT NULL,
  `revocation_reason` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `auth_sessions_refresh_token_hash_unique` (`refresh_token_hash`),
  KEY `auth_sessions_actor_account_active_idx` (`actor_type`,`account_id`,`revoked_at`,`expires_at`),
  KEY `auth_sessions_expires_at_idx` (`expires_at`),
  KEY `auth_sessions_revoked_at_idx` (`revoked_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='인증 리프레시 세션 테이블';

CREATE TABLE `password_reset_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token_hash` char(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `issued_at` timestamp(6) NOT NULL,
  `expires_at` timestamp(6) NOT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `password_reset_tokens_actor_email_unique` (`actor_type`,`email`),
  KEY `password_reset_tokens_expires_at_index` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Actor 공통 비밀번호 재설정 토큰 테이블';

INSERT INTO staff_roles (name, display_name) VALUES
    ('platform.super_admin', '최고 관리자'),
    ('platform.admin', '관리자'),
    ('platform.staff', '운영자'),
    ('platform.dev', '개발자');

INSERT INTO staff_permissions (code, display_name) VALUES
    ('common.access', '관리자 접근'),
    ('common.dashboard.show', '대시보드 조회'),
    ('common.profile.show', '내 프로필 조회'),
    ('common.profile.update', '내 프로필 수정'),
    ('platform.agency.create', '에이전시 등록'),
    ('platform.agency.delete', '에이전시 삭제'),
    ('platform.agency.show', '에이전시 조회'),
    ('platform.agency.update', '에이전시 수정'),
    ('platform.category.manage', '카테고리 관리'),
    ('platform.expert.create', '전문가 등록'),
    ('platform.expert.delete', '전문가 삭제'),
    ('platform.expert.show', '전문가 조회'),
    ('platform.expert.update', '전문가 수정'),
    ('platform.faq.create', 'FAQ 등록'),
    ('platform.faq.delete', 'FAQ 삭제'),
    ('platform.faq.show', 'FAQ 조회'),
    ('platform.faq.update', 'FAQ 수정'),
    ('platform.hashtag.manage', '해시태그 관리'),
    ('platform.notice.create', '공지 등록'),
    ('platform.notice.delete', '공지 삭제'),
    ('platform.notice.show', '공지 조회'),
    ('platform.notice.update', '공지 수정'),
    ('platform.partner_entry.show', '입점신청 조회'),
    ('platform.partner_entry.update', '입점신청 수정'),
    ('platform.partner_evaluation.show', '파트너 평가 조회'),
    ('platform.partner_evaluation.update', '파트너 평가 수정'),
    ('platform.partner_event_ad.create', '파트너 이벤트 광고 등록'),
    ('platform.partner_event_ad.delete', '파트너 이벤트 광고 삭제'),
    ('platform.partner_event_ad.show', '파트너 이벤트 광고 조회'),
    ('platform.partner_event_ad.update', '파트너 이벤트 광고 수정'),
    ('platform.partner_event_db.show', '이벤트 상담 DB 조회'),
    ('platform.partner_event_db.update', '이벤트 상담 DB 수정'),
    ('platform.partner_event_real_model_db.show', '리얼모델 DB 조회'),
    ('platform.partner_event_real_model_db.update', '리얼모델 DB 수정'),
    ('platform.partner_event.create', '파트너 이벤트 등록'),
    ('platform.partner_event.delete', '파트너 이벤트 삭제'),
    ('platform.partner_event.show', '파트너 이벤트 조회'),
    ('platform.partner_event.update', '파트너 이벤트 수정'),
    ('platform.partner_review.show', '파트너 후기 조회'),
    ('platform.partner_review.update', '파트너 후기 수정'),
    ('platform.partner.account_status.update', '파트너 관리자 로그인 상태 변경'),
    ('platform.partner.allow_status.update', '파트너 승인상태 변경'),
    ('platform.partner.assign_staff', '파트너 담당 직원 지정'),
    ('platform.partner.create', '파트너 등록'),
    ('platform.partner.delete', '파트너 삭제'),
    ('platform.partner.show', '파트너 조회'),
    ('platform.partner.status.update', '파트너 운영상태 변경'),
    ('platform.partner.update', '파트너 수정'),
    ('platform.reported_chat_message.show', '신고 채팅 조회'),
    ('platform.reported_chat_message.update', '신고 채팅 수정'),
    ('platform.reported_partner_evaluation.show', '신고 평가 조회'),
    ('platform.reported_partner_evaluation.update', '신고 평가 수정'),
    ('platform.reported_partner_review.show', '신고 후기 조회'),
    ('platform.reported_partner_review.update', '신고 후기 수정'),
    ('platform.reported_talk.show', '신고 토크 조회'),
    ('platform.reported_talk.update', '신고 토크 수정'),
    ('platform.reported_video.show', '신고 동영상 조회'),
    ('platform.reported_video.update', '신고 동영상 수정'),
    ('platform.specialist.create', '스페셜리스트 등록'),
    ('platform.specialist.delete', '스페셜리스트 삭제'),
    ('platform.specialist.show', '스페셜리스트 조회'),
    ('platform.specialist.update', '스페셜리스트 수정'),
    ('platform.staff.create', '직원 등록'),
    ('platform.staff.delete', '직원 삭제'),
    ('platform.staff.show', '직원 조회'),
    ('platform.staff.update', '직원 수정'),
    ('platform.talk.create', '토크 등록'),
    ('platform.talk.delete', '토크 삭제'),
    ('platform.talk.show', '토크 조회'),
    ('platform.talk.update', '토크 수정'),
    ('platform.user.show', '회원 조회'),
    ('platform.user.status.update', '회원 상태 수정'),
    ('platform.video.create', '동영상 등록'),
    ('platform.video.delete', '동영상 삭제'),
    ('platform.video.show', '동영상 조회'),
    ('platform.video.update', '동영상 수정');

INSERT INTO staff_role_permissions (staff_role_id, staff_permission_id)
SELECT role.id, permission.id
FROM staff_roles role
CROSS JOIN staff_permissions permission
WHERE role.name = 'platform.super_admin';

INSERT INTO staff_role_permissions (staff_role_id, staff_permission_id)
SELECT role.id, permission.id
FROM staff_roles role
CROSS JOIN staff_permissions permission
WHERE role.name = 'platform.admin'
  AND permission.code NOT LIKE 'platform.staff.%'
  AND permission.code NOT IN (
    'platform.partner.account_status.update',
    'platform.partner.allow_status.update',
    'platform.partner.status.update'
  );

INSERT INTO staff_role_permissions (staff_role_id, staff_permission_id)
SELECT role.id, permission.id
FROM staff_roles role
CROSS JOIN staff_permissions permission
WHERE role.name IN ('platform.staff', 'platform.dev')
  AND permission.code IN (
    'common.access',
    'common.dashboard.show',
    'common.profile.show',
    'common.profile.update',
    'platform.agency.show',
    'platform.expert.show',
    'platform.faq.show',
    'platform.notice.show',
    'platform.partner_entry.show',
    'platform.partner_evaluation.show',
    'platform.partner_event_db.show',
    'platform.partner_event_real_model_db.show',
    'platform.partner_review.show',
    'platform.partner.show',
    'platform.reported_chat_message.show',
    'platform.reported_partner_evaluation.show',
    'platform.reported_partner_review.show',
    'platform.reported_talk.show',
    'platform.reported_video.show',
    'platform.specialist.show',
    'platform.talk.show',
    'platform.user.show'
  );
