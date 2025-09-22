Feature: User Registration
  As a new user
  I want to register an account
  So that I can access the application and track my work

  Background:
    Given the application is running
    And the database is clean

  Scenario: Successful user registration with valid details
    Given I want to register a new user account
    When I provide valid registration details:
      | username | email               | password    |
      | johndoe  | john@example.com    | SecurePass1 |
    Then the registration should be successful
    And I should receive a success message "User registered successfully"
    And the user should be saved in the database

  Scenario: Registration with duplicate username
    Given a user with username "existinguser" already exists
    When I try to register with the same username:
      | username     | email               | password    |
      | existinguser | newemail@example.com| SecurePass1 |
    Then the registration should fail
    And I should receive an error message "Username already exists"

  Scenario: Registration with duplicate email
    Given a user with email "existing@example.com" already exists
    When I try to register with the same email:
      | username | email                | password    |
      | newuser  | existing@example.com | SecurePass1 |
    Then the registration should fail
    And I should receive an error message "Email already exists"

  Scenario Outline: Registration with invalid data
    Given I want to register a new user account
    When I provide invalid registration details:
      | username   | email   | password   |
      | <username> | <email> | <password> |
    Then the registration should fail
    And I should receive an error message "<error_message>"

    Examples:
      | username | email           | password | error_message                           |
      |          | test@email.com  | Pass123  | Username is required                    |
      | test     |                 | Pass123  | Email is required                       |
      | test     | test@email.com  |          | Password is required                    |
      | ab       | test@email.com  | Pass123  | Username must be at least 3 characters |
      | test     | invalid-email   | Pass123  | Email should be valid                   |
      | test     | test@email.com  | 123      | Password must be at least 6 characters |

  Scenario: Registration with very long username
    Given I want to register a new user account
    When I provide a username longer than 20 characters:
      | username                     | email           | password    |
      | verylongusernamethatexceeds  | test@email.com  | SecurePass1 |
    Then the registration should fail
    And I should receive an error message "Username must be at most 20 characters"