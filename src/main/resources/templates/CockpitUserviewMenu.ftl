<div class="page-content">
    <div class="page-header">
        <h1>
            Testing
            <small>
                <i class="ace-icon fa fa-angle-double-right"></i>
                overview & stats
            </small>
        </h1>
    </div>
    <div class="row">
        <#list renderedMenus as item>
            <#assign title = item['properties']['label'] >
            <#assign renderPage = item['renderPage'] >
            <#assign columnSize = item['columnSize'] >
            <div class="${columnSize!}">
                <div class="widget-box transparent">
                    <div class="widget-header widget-header-flat">
                        <h4 class="widget-title lighter"> ${title!} </h4>
                        <div class="widget-toolbar">
                            <a href="#" data-action="collapse">
                                <i class="ace-icon fa fa-chevron-up"></i>
                            </a>
                        </div>
                    </div>
                    <div class="widget-body">
                        <div class="widget-main no-padding">
                            ${renderPage!}
                        </div>
                    </div>
                </div>
            </div>

        </#list>
    </div>
     <div class="clearfix"></div>
</div>