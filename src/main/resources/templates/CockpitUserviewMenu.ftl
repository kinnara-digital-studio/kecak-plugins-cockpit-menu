<table class="xrounded_shadowed footable-loaded footable" id="spd_dashboard">
    <thead>
        <tr>
            <th class="">Testing</th>
        </tr>
    </thead>
    <tbody>
        <#list renderedMenus!?keys as key>
            <#assign title = renderedMenus[key]['properties']['label'] >
            <#assign renderPage = renderedMenus[key]['renderPage'] >

            <tr>
                <td><h1>${title!}</h1></td>
            </tr>
            <tr>
                <td>
                    ${renderPage!}
                </td>
            </tr>
        </#list>
    </tbody>
</table>