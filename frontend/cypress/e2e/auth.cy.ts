describe('Auth flow', () => {
  const teacherToken =
    'eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiVEVBQ0hFUiJ9.signature'

  const pupilToken =
    'eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiUFVQSUwifQ.signature'

  beforeEach(() => {
    cy.clearCookies()
    cy.clearLocalStorage()
    cy.clearAllSessionStorage()

    cy.intercept('POST', '/api/auth/refresh', {
      statusCode: 200,
      body: {
        accessToken: teacherToken,
      },
    }).as('refresh')

    cy.intercept('GET', '/api/classrooms', {
      statusCode: 200,
      body: [],
    }).as('classrooms')

    cy.intercept('GET', '/api/accounts/me', {
      statusCode: 200,
      body: {
        role: 'PUPIL',
        classroomMembershipStatus: null,
      },
    }).as('me')
  })

  it('opens home page and navigates to sign in', () => {
    cy.visit('/')

    cy.contains('Er du klar for oppdraget?').should('be.visible')
    cy.contains('Logg inn').click()

    cy.url().should('include', '/sign-in')
    cy.contains('Logg inn med Feide').should('be.visible')
  })

  it('logs in as teacher and redirects to teacher classrooms', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      body: {
        accessToken: teacherToken,
      },
    }).as('login')

    cy.visit('/sign-in')

    cy.contains('Bruk arbeids- eller skolekonto').click()
    cy.get('input[type="email"]').type('teacher@ntnu.no')
    cy.contains('Neste').click()

    cy.get('input[type="password"]').type('Password1')
    cy.get('button[type="submit"]').contains('Logg på').click()

    cy.wait('@login').its('request.body').should('deep.equal', {
      email: 'teacher@ntnu.no',
      password: 'Password1',
    })

    cy.url().should('include', '/teacher/classrooms')
  })

  it('shows login error when backend rejects credentials', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 401,
      body: {
        error: 'Feil e-post eller passord',
      },
    }).as('loginFail')

    cy.visit('/sign-in')

    cy.contains('Bruk arbeids- eller skolekonto').click()
    cy.get('input[type="email"]').type('wrong@ntnu.no')
    cy.contains('Neste').click()

    cy.get('input[type="password"]').type('WrongPassword1')
    cy.get('button[type="submit"]').contains('Logg på').click()

    cy.wait('@loginFail')
    cy.contains('Feil e-post eller passord').should('be.visible')
  })

  it('navigates from sign in to sign up', () => {
    cy.visit('/sign-in')

    cy.contains('Bruk arbeids- eller skolekonto').click()
    cy.contains('Sign up').click()

    cy.url().should('include', '/sign-up')
    cy.contains('Opprett bruker').should('be.visible')
  })

  it('signs up as teacher and redirects to teacher classrooms', () => {
    cy.intercept('POST', '/api/auth/signup', {
      statusCode: 200,
      body: {
        accessToken: teacherToken,
      },
    }).as('signup')

    cy.visit('/sign-up')

    cy.get('#reg-legal-name').type('Test Teacher')
    cy.get('#reg-email').type('teacher@ntnu.no')
    cy.get('#reg-password').type('Password1')
    cy.get('#reg-confirm-password').type('Password1')
    cy.get('#reg-role').select('Lærer')

    cy.contains('Registrer meg').click()

    cy.wait('@signup').its('request.body').should('deep.equal', {
      email: 'teacher@ntnu.no',
      password: 'Password1',
      legalName: 'Test Teacher',
      role: 'TEACHER',
    })

    cy.url().should('include', '/teacher/classrooms')
  })

  it('signs up as pupil and redirects according to pupil auth route', () => {
    cy.intercept('POST', '/api/auth/signup', {
      statusCode: 200,
      body: {
        accessToken: pupilToken,
      },
    }).as('signup')

    cy.visit('/sign-up')

    cy.get('#reg-legal-name').type('Test Pupil')
    cy.get('#reg-email').type('pupil@ntnu.no')
    cy.get('#reg-password').type('Password1')
    cy.get('#reg-confirm-password').type('Password1')
    cy.get('#reg-role').select('Elev')

    cy.contains('Registrer meg').click()

    cy.wait('@signup').its('request.body').should('deep.equal', {
      email: 'pupil@ntnu.no',
      password: 'Password1',
      legalName: 'Test Pupil',
      role: 'PUPIL',
    })

    cy.url().should('include', '/join-classroom')
  })

  it('shows signup validation errors before calling backend', () => {
    cy.visit('/sign-up')

    cy.contains('Registrer meg').click()
    cy.contains('Du må skrive inn fullt navn.').should('be.visible')

    cy.get('#reg-legal-name').type('Test User')
    cy.get('#reg-email').type('not-an-email')
    cy.contains('Registrer meg').click()
    cy.contains('Du må skrive inn en gyldig e-postadresse.').should('be.visible')

    cy.get('#reg-email').clear().type('test@ntnu.no')
    cy.get('#reg-password').type('short')
    cy.get('#reg-confirm-password').type('short')
    cy.contains('Registrer meg').click()
    cy.contains('Passordet må være minst 8 tegn.').should('be.visible')
  })

  it('shows signup error when passwords do not match', () => {
    cy.visit('/sign-up')

    cy.get('#reg-legal-name').type('Test User')
    cy.get('#reg-email').type('test@ntnu.no')
    cy.get('#reg-password').type('Password1')
    cy.get('#reg-confirm-password').type('Password2')

    cy.contains('Passordene er ikke like.').should('be.visible')

    cy.contains('Registrer meg').click()
    cy.contains('Passordene er ikke like.').should('be.visible')
  })

  it('shows backend signup error', () => {
    cy.intercept('POST', '/api/auth/signup', {
      statusCode: 400,
      body: {
        error: 'E-post er allerede i bruk',
      },
    }).as('signupFail')

    cy.visit('/sign-up')

    cy.get('#reg-legal-name').type('Test User')
    cy.get('#reg-email').type('used@ntnu.no')
    cy.get('#reg-password').type('Password1')
    cy.get('#reg-confirm-password').type('Password1')

    cy.contains('Registrer meg').click()

    cy.wait('@signupFail')
    cy.contains('E-post er allerede i bruk').should('be.visible')
  })
})
