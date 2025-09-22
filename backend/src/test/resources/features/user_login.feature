Feature: User Login
  As a registered user
  I want to login to my account
  So that I can access the application features

  Background:
    Given the application is running
    And the database is clean
    And the following users exist:
      | username  | email            | password    |
      | testuser  | test@example.com | SecurePass1 |
      | johnsmith | john@smith.com   | MyPass123   |

  Scenario: Successful login with valid username and password
    Given I want to login to my account
    When I provide valid login credentials:
      | username | password    |
      | testuser | SecurePass1 |
    Then the login should be successful
    And I should receive a success message "Login successful"
    And I should receive an authentication token

  Scenario: Successful login with email instead of username
    Given I want to login to my account
    When I provide valid login credentials using email:
      | email            | password    |
      | test@example.com | SecurePass1 |
    Then the login should be successful
    And I should receive a success message "Login successful"
    And I should receive an authentication token

  Scenario: Login with incorrect password
    Given I want to login to my account
    When I provide incorrect login credentials:
      | username | password     |
      | testuser | WrongPass123 |
    Then the login should fail
    And I should receive an error message "Invalid username or password"
    And no authentication token should be provided

  Scenario: Login with non-existent username
    Given I want to login to my account
    When I provide login credentials for non-existent user:
      | username     | password    |
      | nonexistent  | AnyPass123  |
    Then the login should fail
    And I should receive an error message "Invalid username or password"
    And no authentication token should be provided

  Scenario Outline: Login with invalid or missing data
    Given I want to login to my account
    When I provide incomplete login credentials:
      | username   | password   |
      | <username> | <password> |
    Then the login should fail
    And I should receive an error message "<error_message>"

    Examples:
      | username | password    | error_message              |
      |          | SecurePass1 | Username is required       |
      | testuser |             | Password is required       |
      |          |             | Username and password are required |

  Scenario: Login with valid credentials multiple times
    Given I want to login to my account
    When I login successfully with valid credentials:
      | username | password    |
      | testuser | SecurePass1 |
    And I login again with the same credentials:
      | username | password    |
      | testuser | SecurePass1 |
    Then both login attempts should be successful
    And I should receive authentication tokens for both attempts

  Scenario: Check user data after successful login
    Given I want to login to my account
    When I login successfully with valid credentials:
      | username | password    |
      | johnsmith | MyPass123  |
    Then the login should be successful
    And the returned user data should contain:
      | field    | value          |
      | username | johnsmith      |
      | email    | john@smith.com |
    And the password should not be exposed in the response