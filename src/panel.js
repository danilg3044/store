Ext.define('User', {
    extend: 'Ext.data.Model',
              
     idProperty: 'userID',
              
     fields: [{
         name: 'userID',
         type: 'int'
     }, {
         name: 'name',
         type: 'string'
     }, {
         name: 'surname',
         type: 'string'
     }, {
         name: 'date',
         type: 'date'
     }, {
         name: 'email',
         type: 'string'
     }, {
         name: 'married',
         type: 'bool'
     }]
});

var store = Ext.create('Ext.data.Store', {
             model: 'User',
             autoLoad: true,
             proxy: {
                     type: 'ajax',
                     url: 'users.json',
                     reader: {
                         type: 'json',
                         root: 'users'
                     }
         }
 });
 
 
 var grid = Ext.create('Ext.grid.Panel', {
    title: 'Пользователи',
    height: 200,
    width: 600,
    store: store,
    columns: [{
        header: 'Имя',
        dataIndex: 'name'
    }, {
        header: 'Фамилия',
        dataIndex: 'surname'
    }, {
        header: 'Дата рождения',
        dataIndex: 'date',
        xtype:'datecolumn',
        format: 'd/m/Y',
        flex:1
    }, {
        header: 'E-mail',
        dataIndex: 'email',
        flex:1
    }, {
        header: 'Женат/Замужем',
        dataIndex: 'married',
        flex:1
    }],
    renderTo: Ext.getBody()
});    

var btn = Ext.create('Ext.Button', {
    text: 'Нажми',
    height:30,
    margin: '50 0 0 50',
    renderTo: Ext.getBody(),
    handler: function() {
        var tran = '<table><tr><td>11</td><td>12</td></tr><tr><td>21</td><td>22</td></tr></table>';
        panel.update(tran);
    }
});

var key1Label = Ext.create('Ext.form.Label', {
    fieldLabel : 'keyOne',
    width : 50
});

var val1Label = Ext.create('Ext.form.Label', {
    fieldLabel : 'valueOne',
    width : 50
});

var panel=Ext.create('Ext.panel.Panel', {
    title:'Приложение ExtJS 4',
    width: 300,
    height: 150,
    padding:10,
    bodyPadding:5,
    bodyStyle:{"background-color":"red"}, 
    renderTo: Ext.getBody()
});

 Ext.application({
    name: 'HelloExt',
    launch: function() {
        Ext.create('Ext.Panel', {
            title: 'Форма ввода',
            width: 600,
            autoHeight: true,
            bodyPadding: 10,
            defaults: {
                labelWidth: 100
            },
            items: [grid, btn, panel],
            renderTo: Ext.getBody()
        });
    }
});