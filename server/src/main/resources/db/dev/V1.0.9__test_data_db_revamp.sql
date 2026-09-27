-- login is jsmith/test
insert into
  users(
    enabled,
    username,
    email,
    password,
    role_role_id, 
    user_id
  )
values
(
    TRUE,
    'jsmith',
    'jsmith@google.com',
    '$2a$10$uhmwgpRI0vxF51s8pxt94Ojs8Cwrg7uLwhf3sK7EYv1i7QLML7aJ6', -- pwd: test
    0,
    1
  );

insert into
  users(
    enabled,
    username,
    email,
    password,
    role_role_id,
    user_id
  )
values
(
    TRUE,
    'king',
    'king@google.com',
    '$2a$10$uhmwgpRI0vxF51s8pxt94Ojs8Cwrg7uLwhf3sK7EYv1i7QLML7aJ6', -- pwd: test
    0,
    2 
  );

insert into
  users(
    enabled,
    username,
    email,
    password,
    role_role_id,
    user_id
  )
values
(
    TRUE,
    'linus',
    'linus@google.com',
    '$2a$10$uhmwgpRI0vxF51s8pxt94Ojs8Cwrg7uLwhf3sK7EYv1i7QLML7aJ6', -- pwd: test
    0,
    30
  );

-- jsmith's bookmarks
-- id: 1
insert into
  bookmark (id, title, url, user_id, scrapable)
values
( 
    1,
    'Best Cheesecake Recipe',
    'https://sugarspunrun.com/best-cheesecake-recipe/',
    1, 
    true
  );

-- id: 2
insert into
  bookmark (id, title, url, user_id, scrapable)
values
(
    2,
    'Dark mode guide',
    'https://blog.logrocket.com/dark-mode-react-in-depth-guide/',
    1, 
    true
  );

-- id: 3
insert into
  bookmark (id, title, url, user_id, scrapable)
values
(
    3,
    'Chicken Parm',
    'https://www.foodnetwork.com/recipes/bobby-flay/chicken-parmigiana-recipe-1952359',
    1, 
    true
  );

-- id: 24
insert into
    bookmark (id, title, url, user_id, scrapable)
values
(
  24,
  'Best Cheesecake Recipe2',
  'https://sugarspunrun.com/best-cheesecake-recipe/',
  30,
  true
);

-- id: 25
insert into
  bookmark (id, title, url, user_id, scrapable)
values
(
    25,
    'Best Cheesecake Recipe3',
    'https://sugarspunrun.com/best-cheesecake-recipe/',
    30,
    true
);

-- id: 26
insert into
  bookmark (id, title, url, user_id, scrapable)
values
(
    26,
    'Best Cheesecake Recipe4',
    'https://sugarspunrun.com/best-cheesecake-recipe/',
    30,
    true
);

-- id: 27
insert into
  bookmark (id, title, url, user_id, scrapable)
values
(
    27,
    'Best Cheesecake Recipe5',
    'https://sugarspunrun.com/best-cheesecake-recipe/',
    30,
    true
);

-- id: 28
insert into
  bookmark (id, title, url, user_id, scrapable)
values
(
    28,
    'Best Cheesecake Recipe6',
    'https://sugarspunrun.com/best-cheesecake-recipe/',
    30,
    true
);

-- id: 29
INSERT INTO bookmark (id, title, url, user_id, scrapable)
VALUES (
    29,
    'Ultimate Chocolate Cake',
    'https://sallysbakingaddiction.com/triple-chocolate-layer-cake/',
    30,
    true
);

-- id: 30
INSERT INTO bookmark (id, title, url, user_id, scrapable)
VALUES (
    30,
    'Top 10 Travel Destinations',
    'https://www.lonelyplanet.com/articles/top-travel-destinations',
    30,
    true
);

-- id: 31
INSERT INTO bookmark (id, title, url, user_id, scrapable)
VALUES (
    31,
    'Effective Java Programming',
    'https://www.oreilly.com/library/view/effective-java-3rd/',
    30,
    true
);

-- id: 32
INSERT INTO bookmark (id, title, url, user_id, scrapable)
VALUES (
    32,
    'Healthy Meal Plans',
    'https://www.eatingwell.com/category/4286/meal-plans/',
    30,
    true
);

-- id: 33
INSERT INTO bookmark (id, title, url, user_id, scrapable)
VALUES (
    33,
    'Best Running Shoes 2024',
    'https://www.runnersworld.com/gear/a20865505/best-running-shoes/',
    30,
    true
);

-- id: 34
INSERT INTO bookmark (id, title, url, user_id, scrapable)
VALUES (
    34,
    'Beginner’s Guide to Investing',
    'https://www.investopedia.com/investing-4427765',
    30,
    true
);

-- id: 35
INSERT INTO bookmark (id, title, url, user_id, scrapable)
VALUES (
    35,
    'How to Brew the Perfect Coffee',
    'https://www.javapresse.com/blogs/buying-coffee/how-to-make-coffee',
    30,
    true
);

-- id: 36
INSERT INTO bookmark (id, title, url, user_id, scrapable)
VALUES (
    36,
    'Essential Hiking Gear',
    'https://www.rei.com/learn/expert-advice/day-hiking-checklist.html',
    30,
    true
);

-- id: 37
INSERT INTO bookmark (id, title, url, user_id, scrapable)
VALUES (
    37,
    'Mastering React',
    'https://react.dev/learn',
    30,
    true
);

-- id: 38
INSERT INTO bookmark (id, title, url, user_id, scrapable)
VALUES (
    38,
    'Top 5 Budget Travel Tips',
    'https://www.nomadicmatt.com/travel-tips/',
    30,
    true
);

-- id: 39
INSERT INTO bookmark (id, title, url, user_id, scrapable)
VALUES (
    39,
    'Home Workout Routines',
    'https://www.nerdfitness.com/blog/beginner-bodyweight-workout/',
    30,
    true
);

-- id: 40
INSERT INTO bookmark (id, title, url, user_id, scrapable)
VALUES (
    40,
    'Best Sci-Fi Books of All Time',
    'https://www.penguinrandomhouse.com/the-best-sci-fi-books-of-all-time',
    1,
    true
);

-- id: 41
INSERT INTO bookmark (id, title, url, user_id, scrapable)
VALUES (
    41,
    'Understanding Stock Markets',
    'https://www.wallstreetmojo.com/stock-market-basics/',
    1,
    true
);

-- id: 42
INSERT INTO bookmark (id, title, url, user_id, scrapable)
VALUES (
    42,
    'Gourmet Pizza Recipes',
    'https://www.bonappetit.com/gallery/pizza-recipes',
    1,
    true
);

-- id: 43
INSERT INTO bookmark (id, title, url, user_id, scrapable)
VALUES (
    43,
    'The Best Coding Practices',
    'https://stackify.com/top-20-coding-best-practices/',
    1,
    true
);

-- king's bookmark 
-- id: 4
insert into
  bookmark (id, title, url, user_id, scrapable)
values
(
    4,
    'Favorite Chicken Parm',
    'https://www.foodnetwork.com/recipes/bobby-flay/chicken-parmigiana-recipe-1952359',
    2, 
    true
  );

-- jsmith tags
-- id:   1
insert into
  tag(id, tag_title, user_id)
values
(1, 'Cooking', 1);

-- id:   2
insert into
  tag(id, tag_title, user_id)
values
(2, 'web_dev', 1);

-- id:   3
insert into
  tag(id, tag_title, user_id)
values
(3, 'deserts', 1);

-- id:   4
insert into
  tag(id, tag_title, user_id)
values
(4, 'camping', 1);

-- id:   5
insert into
  tag(id, tag_title, user_id)
values
(5, 'spring docs', 1);

-- id:   6
insert into
  tag(id, tag_title, user_id)
values
(6,'web docs', 1);

-- Cooking related bookmark_tags
insert into
  bookmark_tag(bookmark_id, tag_id)
values
(1, 1);

insert into
  bookmark_tag(bookmark_id, tag_id)
values
(1, 3);

insert into
  bookmark_tag(bookmark_id, tag_id)
values
(3, 1);

-- tech related bookmark_tags
insert into
  bookmark_tag(bookmark_id, tag_id)
values
(2, 2);
------------------------------------

------------------------------------
-- king 
------------------------------------
-- id: 7
insert into
  tag(id, tag_title, user_id)
values
(7, 'cooking', 2);

-- id: 8
insert into
  tag(id, tag_title, user_id)
values
(8, 'wishlist', 2);

insert into
  bookmark_tag
values
(4, 7);


