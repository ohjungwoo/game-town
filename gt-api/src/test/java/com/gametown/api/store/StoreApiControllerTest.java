package com.gametown.api.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametown.account.domain.AccountDto;
import com.gametown.account.enc.AES256Machine;
import com.gametown.api.account.AccountView;
import com.gametown.api.exception.ErrorController;
import com.gametown.store.domain.StoreDto;
import com.gametown.store.domain.StoreFormDto;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static com.gametown.ApiDocumentUtils.getDocumentRequest;
import static com.gametown.ApiDocumentUtils.getDocumentResponse;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class StoreApiControllerTest {
    private MockMvc mockMvc;

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

    @InjectMocks
    private StoreApiController storeApiController;

    @Mock
    private StoreApiService storeApiService;

    @Mock
    private AES256Machine aes256Machine;

    private ObjectMapper objectMapper;

    private AccountDto loginAccount;

    @Before
    public void setup() {
        this.loginAccount = new AccountDto();
        this.mockMvc = MockMvcBuilders.standaloneSetup(storeApiController)
                .apply(documentationConfiguration(restDocumentation))
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .setControllerAdvice(new ErrorController())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return AccountDto.class.equals(parameter.getParameterType());
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
                        return loginAccount;
                    }
                })
                .alwaysDo(print())
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Test
    public void testCreateStore() throws Exception {
        StoreFormDto storeForm = new StoreFormDto();
        storeForm.setName("store name");
        storeForm.setStoreCode("store code");
        storeForm.setAddress("address");

        AccountDto accountDto = new AccountDto();
        StoreDto storeDto = new StoreDto();

        accountDto.setEmail("email");

        storeDto.setStoreId(1L);
        storeDto.setName("store name");
        storeDto.setStoreCode("store code");
        storeDto.setAddress("address");

        StoreCreateView storeCreateView = new StoreCreateView(AccountView.from(accountDto), StoreView.from(storeDto));

        when(storeApiService.create(any(), any()))
                .thenReturn(storeCreateView);

        mockMvc.perform(
                post("/stores")
                        .content(objectMapper.writeValueAsString(storeForm))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("login", "abcd")
        )
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.hostAccount.email", is(equalTo(accountDto.getEmail()))))
                .andExpect(jsonPath("$.store.name", is(equalTo(storeDto.getName()))))
                .andExpect(jsonPath("$.store.storeCode", is(equalTo(storeDto.getStoreCode()))))
                .andExpect(jsonPath("$.store.address", is(equalTo(storeDto.getAddress()))))
                .andDo(document("store-create", getDocumentRequest(), getDocumentResponse(),
                        requestHeaders(headerWithName("login").description("로그인 토큰")),
                        requestFields(
                                fieldWithPath("storeCode").type(JsonFieldType.STRING).description("상점 코드"),
                                fieldWithPath("name").type(JsonFieldType.STRING).description("상점 이름"),
                                fieldWithPath("address").type(JsonFieldType.STRING).description("주소")
                        ),
                        responseFields(
                                fieldWithPath("hostAccount.email").type(JsonFieldType.STRING).description("상점 사장님 이메일"),
                                fieldWithPath("store.name").type(JsonFieldType.STRING).description("상점 이름"),
                                fieldWithPath("store.storeCode").type(JsonFieldType.STRING).description("상점 코드"),
                                fieldWithPath("store.address").type(JsonFieldType.STRING).description("상점 주소")
                        )));
    }

}