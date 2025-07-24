<head>
    <style>
        .widget-body {
            max-height: 128rem;
            overflow: hidden;
            transition: max-height 0.4s ease;
        }

        .widget-body.collapsed {
            max-height: 0;
        }
    </style>
</head>
<body>
<div class="page-content">
    <#if (element.properties.showLabel!"") == "true" >
        <div class="page-header">
            <h1>${element.properties.label!}</h1>
        </div>
    </#if>
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
    <script>
        document.addEventListener('DOMContentLoaded', function () {
            document.querySelectorAll('[data-action="collapse"]').forEach(function (toggle) {
                toggle.addEventListener('click', function (e) {
                    e.preventDefault();

                    const icon = this.querySelector('i');
                    const widgetBox = this.closest('.widget-box');
                    const widgetBody = widgetBox.querySelector('.widget-body');

                    const isCollapsed = widgetBody.classList.contains('collapsed');

                    if (isCollapsed) {
                        // Expand: set to scrollHeight for smooth open
                        widgetBody.classList.remove('collapsed');
                        const scrollHeight = widgetBody.scrollHeight;
                        widgetBody.style.maxHeight = scrollHeight + 'px';

                        icon.classList.remove('fa-chevron-down');
                        icon.classList.add('fa-chevron-up');

                        // Reset max-height after animation
                        setTimeout(() => {
                            widgetBody.style.maxHeight = '';
                        }, 400);
                    } else {
                        // Collapse: set to current height then animate to 0
                        const currentHeight = widgetBody.scrollHeight;
                        widgetBody.style.maxHeight = currentHeight + 'px';
                        // allow repaint
                        requestAnimationFrame(() => {
                            widgetBody.style.maxHeight = '0px';
                            widgetBody.classList.add('collapsed');
                        });

                        icon.classList.remove('fa-chevron-up');
                        icon.classList.add('fa-chevron-down');
                    }
                });
            });
        });
    </script>
</body>