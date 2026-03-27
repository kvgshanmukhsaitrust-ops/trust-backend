INSERT INTO impact_stats (category, current_value, unit, icon) VALUES 
('WATER', 50000, 'Liters', '💧'),
('TREES', 1200, 'Trees', '🌳'),
('STUDENTS', 500, 'Students', '🎓'),
('CARRIAGES', 50, 'Carriages', '🛒')
ON DUPLICATE KEY UPDATE current_value = VALUES(current_value);
-- Adding initial success stories (Added created_at and updated_at)
INSERT INTO success_stories (title, description, image_url, category, created_at, updated_at) 
VALUES (
    'Clean Water for Rural Schools', 
    'Successfully installed a high-capacity water filtration plant in the Zilla Parishad High School, providing safe drinking water to over 500 students daily.', 
    'https://images.unsplash.com/photo-1541250357608-86d34b413982', 
    'Health',
    NOW(), 
    NOW()
) ON DUPLICATE KEY UPDATE title=title;

INSERT INTO success_stories (title, description, image_url, category, created_at, updated_at) 
VALUES (
    '1000 Trees Plantation Drive', 
    'Collaborated with 100+ volunteers to plant 1000 saplings across the local community park and highway stretches to improve green cover.', 
    'https://images.unsplash.com/photo-1542601906990-b4d3fb778b09', 
    'Environment',
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE title=title;

-- Use 'trust_members' to match your @Entity @Table(name = "trust_members")
-- Updated INSERT to match the TrustMember.java Entity fields
INSERT INTO trust_members (name, role, tagline, bio, image_url, display_order) VALUES 
('K.Prakasa rao', 'Founder & Chairman', 'Visionary Leader', 'Arjun has 15 years of experience in social work and led the initial water project in 2018.', 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e', 1),
(' Krishna Priya', 'Treasurer', 'Operations Expert', 'Priya handles financial transparency and ensures every donation reaches the intended cause.', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330', 2),
('David raju', 'Vice president', 'Community Builder', 'Rajesh manages our network of 500+ volunteers across the state for emergency relief.', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e', 3),
('K.Rohith vijay', 'Seceretary', 'Academic Advocate', 'A former teacher, Anjali oversees our student scholarship programs and rural school kits.', 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80', 4),
('Naghul khan', 'Volunteer co-ordinator', 'Nature Guardian', 'Vikram leads our reforestation drives and urban greening initiatives.', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d', 5)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Default system settings
INSERT INTO system_settings (setting_key, setting_value)
VALUES ('HOME_HERO_IMAGE', 'https://images.unsplash.com/photo-1488521787991-ed7bbaae773c')
ON DUPLICATE KEY UPDATE setting_key=setting_key;