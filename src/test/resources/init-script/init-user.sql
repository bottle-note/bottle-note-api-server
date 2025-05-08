insert into users (email, nick_name, age, image_url, gender, role, status, social_type,
				   create_at, last_modify_at)
values ('hyejj19@naver.com', 'WOzU6J8541', null, null, 'FEMALE', 'ROLE_USER', 'ACTIVE', '[
	"KAKAO"
]',
		null, null),
	   ('chadongmin@naver.com', 'xIFo6J8726', null, null, 'MALE', 'ROLE_USER', 'ACTIVE',
		'[
			"KAKAO"
		]',
		null, null),
	   ('dev.bottle-note@gmail.com', 'PARC6J8814', 25, null, 'MALE', 'ROLE_USER', 'ACTIVE',
		'[
			"GOOGLE"
		]',
		null, null),
	   ('eva.park@oysterable.com', 'VOKs6J8831', null, null, 'FEMALE', 'ROLE_USER', 'ACTIVE',
		'[
			"GOOGLE"
		]',
		null, null),
	   ('rlagusrl928@gmail.com', 'hpPw6J111837', null, null, 'MALE', 'ROLE_USER', 'ACTIVE',
		'[
			"KAKAO"
		]',
		null, null),
	   ('ytest@gmail.com', 'OMkS6J12123', null, null, 'MALE', 'ROLE_USER', 'ACTIVE', '[
		   "KAKAO"
	   ]',
		null, null),
	   ('juye@gmail.com', 'juye12', null,
		'{ "viewUrl": "http://example.com/new-profile-image.jpg" }', 'MALE', 'ROLE_USER', 'ACTIVE',
		'[
			"GOOGLE"
		]',
		null, null),
	   ('rkdtkfma@naver.com', 'iZBq6J22547', null, null, null, 'ROLE_USER', 'ACTIVE', '[
		   "KAKAO"
	   ]',
		null, null)
ON DUPLICATE KEY UPDATE email = email;
