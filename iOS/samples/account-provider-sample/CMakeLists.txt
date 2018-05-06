set(sources
    src/AADAccountProvider.m
    src/AADMSAAccountProvider.m
    src/MSAAccountProvider.m
    src/MSATokenCache.m
    src/MSATokenRequest.m
)

file(GLOB headers LIST_DIRECTORIES false include/*.h src/*.h)

foreach(source IN LISTS sources)
    if(${source} MATCHES "\.mm$")
        set_source_files_properties(${source} PROPERTIES COMPILE_FLAGS "-x objective-c++ -fobjc-arc")
    elseif(${source} MATCHES "\.m$")
        set_source_files_properties(${source} PROPERTIES COMPILE_FLAGS "-fobjc-arc")
    endif()
endforeach()

add_library(sample_account_providers STATIC ${sources} ${headers})

add_dependencies(sample_account_providers cdp_one_sdk_ios ADAL)

target_include_directories(sample_account_providers PRIVATE 
    $<TARGET_FILE_DIR:cdp_one_sdk_ios>/Headers
    $<TARGET_FILE_DIR:ADAL>/Headers)

set_target_properties(sample_account_providers PROPERTIES
    XCODE_ATTRIBUTE_FRAMEWORK_SEARCH_PATHS "\$(inherited) $<TARGET_FILE_DIR:cdp_one_sdk_ios>/.. $<TARGET_FILE_DIR:ADAL>/..")

target_link_libraries(sample_account_providers PRIVATE
    "-framework $<TARGET_PROPERTY:cdp_one_sdk_ios,NAME>"
    "-framework $<TARGET_PROPERTY:ADAL,NAME>")

target_include_directories(sample_account_providers
    PUBLIC
        include
)