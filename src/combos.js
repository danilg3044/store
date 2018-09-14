Ext.onReady(function(){
    var store = new Ext.data.ArrayStore({
        fields: ['abbr', 'state', 'nick'],
        data : [
            ['AL', 'Alabama', 'The Heart of Dixie'],
            ['AK', 'Alaska', 'The Land of the Midnight Sun'],
            ['AZ', 'Arizona', 'The Grand Canyon State'],
            ['AR', 'Arkansas', 'The Natural State']
        ]
    });
 
    var comboWithTooltip = new Ext.form.ComboBox({
        tpl: '<tpl for="."><div>{state}. {nick}</div></tpl>',
        store: store,
        mode: 'local',
        applyTo: 'local-states-with-qtip',
        //triggerAction: 'all',
        listeners: {
            expand : function(combo){
                //var id = Ext.getCmp('grid').getSelectionModel().selection.record.data.id;
                combo.store.clearFilter();
                //combo.store.filter([{property: 'state', value: 'Arkansas'}]);
                combo.store.filterBy(function(rec, id) {
                   return true;
                  });
            }
        }
    });

    new Ext.Panel({
    	contentEl: 'state-combo-qtip-code',
    	autoScroll: true,
    	width: 128,
    	title: 'View code to create this combo',
    	hideCollapseTool: true,
    	titleCollapse: true,
    	collapsible: true,
    	collapsed: true,
        renderTo: 'state-combo-qtip-code-panel'
    });
});