run:
	docker compose build; docker compose up app;

tests:
	docker compose build; docker compose up tests;

stop:
	docker compose down;