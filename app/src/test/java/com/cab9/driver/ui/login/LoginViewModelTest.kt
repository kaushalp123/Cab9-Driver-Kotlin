package com.cab9.driver.ui.login

import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

//    private val loginRepo = mockk<LoginRepository>()
//    private val cab9Repo = mockk<Cab9Repository>()
//    private val errorHandler = mockk<ApiErrorHandler>()
//
//    private lateinit var viewModel: LoginViewModel
//
//    @get:Rule
//    val mainCoroutineRule = MainCoroutineRule()
//
//    @Before
//    fun setup() {
//        MockKAnnotations.init(this, relaxed = true)
//        viewModel = LoginViewModel(loginRepo, cab9Repo, errorHandler)
//    }
//
//    @Test
//    fun `login with valid credential`() = runTest {
//        val response = LoginConfig(
//            accessToken = null,
//            tokenType = "",
//            expiresIn = 0L,
//            userId = "",
//            name = "",
//            username = "",
//            imageUrl = "",
//            email = "",
//            tenantId = "",
//            claims = "",
//            googleApiKey = "",
//            locale = "",
//            timezone = "",
//            isChauffeurModeActive = "",
//            useMetric = "",
//            serverTime = null
//        )
//
//        coEvery { loginRepo.doLogin(any(), any()) } answers {
//            val username = firstArg<String>()
//            val password = secondArg<String>()
//            if (username == "ctd" && password == "Test1234") response
//            else throw Exception()
//        }
//
//        viewModel.doLogin("ctd", "Test1234")
//        Assert.assertEquals(viewModel.loginOutcome.value, Outcome.success(response))
//    }

}