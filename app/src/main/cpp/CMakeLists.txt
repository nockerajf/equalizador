cmake_minimum_required(VERSION 3.16.3)

# 1) Nome do projeto e linguagem
project(ThreeBandFilter
        VERSION 1.0
        DESCRIPTION "Mini biblioteca de filtros low/mid/high para Android"
        LANGUAGES CXX)
# 2) Padrão de C++
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
# 3) Arquivos-fonte e headers públicos
set(SOURCE_FILES
        Biquad.cpp
        ThreeBandFilter.cpp)
set(PUBLIC_HEADERS
        Biquad.h
        ThreeBandFilter.h
        jni_wrapper.cpp)
# 4) Cria a biblioteca compartilhada *.so
add_library(threebandfilter SHARED
        ${SOURCE_FILES}
        ${PUBLIC_HEADERS})
# 5) Exponha os headers para outros módulos (PUBLIC = vai para consumers)
target_include_directories(threebandfilter PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
# 6) Caso use logs do Android (LOGD, __android_log_print etc.)
find_library(ANDROID_LOG_LIB log)
target_link_libraries(threebandfilter
        ${ANDROID_LOG_LIB})
# 7) (Opcional) Se futuramente adicionar Oboe:

# 8) Visibilidade mínima do símbolo (ajuda no tamanho do .so)
if(NOT WIN32)
    # Adiciona flags de otimização e define NDEBUG (remove logs de debug)
       target_compile_options(threebandfilter PRIVATE
            -fvisibility=hidden
            -O2
            -DNDEBUG
    )
endif()